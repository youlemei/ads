package com.lwz.ads.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwz.ads.bean.WeChatRobotMsg;
import com.lwz.ads.constant.ClickStatusEnum;
import com.lwz.ads.constant.Const;
import com.lwz.ads.constant.TraceTypeEnum;
import com.lwz.ads.mapper.ClickRecordMapper;
import com.lwz.ads.mapper.entity.Advertisement;
import com.lwz.ads.mapper.entity.ClickRecord;
import com.lwz.ads.mapper.entity.PromoteRecord;
import com.lwz.ads.service.IClickRecordService;
import com.lwz.ads.service.WeChatRobotService;
import com.lwz.ads.util.DateUtils;
import com.lwz.ads.util.IPUtils;
import com.lwz.ads.util.RedisUtils;
import com.lwz.ads.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * <p>
 * 点击记录, 按日分表 服务实现类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
@Slf4j
@Service
public class ClickRecordServiceImpl extends ServiceImpl<ClickRecordMapper, ClickRecord> implements IClickRecordService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private WeChatRobotService weChatRobotService;

    @Value("${system.web.scheme:http}")
    private String scheme;

    @Value("${system.web.domain:localhost:9999}")
    private String domain;

    @Value("${click_record_create_days:2}")
    private int createDays;

    @Value("${click_record_delete_days_ago:45}")
    private int deleteDaysAgo;

    @Override
    @Transactional
    public ClickRecord saveClick(LocalDateTime clickTime, Map<String, Object> request, String type, PromoteRecord promoteRecord, Advertisement ad) {

        String clickId = UUID.randomUUID().toString().replaceAll("-", "");
        ClickRecord clickRecord = new ClickRecord()
                .setId(clickId)
                .setAdId(ad.getId())
                .setAdCreator(ad.getCreator())
                .setChannelId(promoteRecord.getChannelId())
                .setChannelCreator(promoteRecord.getChannelCreator())
                .setCreateTime(clickTime)
                .setTraceType(type)
                .setParamJson(JSON.toJSONString(request))
                .setClickStatus(ClickStatusEnum.RECEIVED.getStatus());

        //存表, 分表
        String ip = Const.IP;
        String idfa = Const.IDFA;
        String imei = Const.IMEI;
        UriComponents adUri = UriComponentsBuilder.fromHttpUrl(ad.getTraceUrl()).build();
        adUri.getQueryParams().forEach((key, list) -> {
            if (!CollectionUtils.isEmpty(list)) {
                String value = list.get(0);
                if (StringUtils.hasLength(value) && Const.PARAM_PATTERN.matcher(value).matches()) {
                    String placeHolder = value.toLowerCase();
                    if (placeHolder.contains(ip) && request.containsKey(key)) {
                        Optional.ofNullable(request.get(key)).ifPresent(o -> clickRecord.setIp(o.toString()));
                    }
                    if ((placeHolder.contains(idfa) || placeHolder.contains(imei)) && request.containsKey(key)) {
                        Optional.ofNullable(request.get(key)).ifPresent(o -> clickRecord.setMac(o.toString()));
                    }
                }
            }
        });

        String date = clickTime.format(DateUtils.yyyyMMdd);
        getBaseMapper().insertWithDate(clickRecord, date);
        if (promoteRecord.getClickDayLimit() != null && promoteRecord.getClickDayLimit() > 0) {
            redisUtils.execute(redis -> {
                String limitKey = String.format(Const.CLICK_DAY_LIMIT_KEY, date, promoteRecord.getId());
                redis.opsForValue().increment(limitKey, 1);
                redis.expire(limitKey, 7, TimeUnit.DAYS);
            });
        }

        if (ad.getClickDayLimit() != null && ad.getClickDayLimit() > 0) {
            redisUtils.execute(redis -> {
                String adLimitKey = String.format(Const.AD_CLICK_DAY_LIMIT_KEY, date, ad.getId());
                redis.opsForValue().increment(adLimitKey, 1);
                redis.expire(adLimitKey, 7, TimeUnit.DAYS);
            });
        }

        redisUtils.execute(redis -> {
            String amountKey = String.format(Const.CLICK_DAY_AMOUNT, date);
            String pid = promoteRecord.getAdId() + "_" + promoteRecord.getChannelId();
            redis.opsForHash().increment(amountKey, pid, 1);
            redis.expire(amountKey, 7, TimeUnit.DAYS);

            String actualKey = String.format(Const.CLICK_DAY_ACTUAL_AMOUNT, date, pid);
            int hashCode = (clickRecord.getIp() + clickRecord.getMac()).hashCode();
            redis.opsForSet().add(actualKey, hashCode);
            redis.expire(actualKey, 7, TimeUnit.DAYS);
        });
        return clickRecord;
    }

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void asyncHandleClick(ClickRecord clickRecord, Advertisement ad) {
        handleClick(clickRecord, ad);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleClick(ClickRecord clickRecord, Advertisement ad) {

        if (clickRecord.getClickStatus().intValue() != ClickStatusEnum.RECEIVED.getStatus()) {
            return;
        }

        String date = clickRecord.getCreateTime().format(DateUtils.yyyyMMdd);
        UriComponents adUri = buildAdTraceUri(clickRecord.getId(), ad, date, clickRecord);

        ResponseEntity<String> resp = requestTraceUri("asyncHandleClick", adUri, ad);

        if (resp == null || !resp.getStatusCode().is2xxSuccessful()) {
            //增加重试次数
            getBaseMapper().incrRetryTimes(clickRecord.getId(), date);
            return;
        }

        ClickRecord me = new ClickRecord();
        me.setId(clickRecord.getId());
        me.setClickStatus(ClickStatusEnum.RECEIVED.getStatus());
        ClickRecord to = new ClickRecord();
        to.setClickStatus(ClickStatusEnum.UNCONVERTED.getStatus());
        to.setEditor("system");
        to.setEditTime(LocalDateTime.now());
        getBaseMapper().updateWithDate(me, to, date);

        log.info("asyncHandleClick success. date:{} clickId:{}", date, clickRecord.getId());
    }

    @Override
    @Transactional
    public URI redirectHandleClick(ClickRecord clickRecord, Advertisement ad) {

        TraceTypeEnum traceType = TraceTypeEnum.valueOfType(ad.getTraceType());
        if (traceType == TraceTypeEnum.ASYNC) {
            SpringContextHolder.getBean(IClickRecordService.class).asyncHandleClick(clickRecord, ad);
            return URI.create(ad.getPreviewUrl());
        }

        if (traceType == TraceTypeEnum.REDIRECT) {

            String date = clickRecord.getCreateTime().format(DateUtils.yyyyMMdd);
            UriComponents adUri = buildAdTraceUri(clickRecord.getId(), ad, date, clickRecord);

            ResponseEntity<String> resp = requestTraceUri("redirectHandleClick", adUri, ad);

            ClickRecord to = new ClickRecord();
            to.setId(clickRecord.getId());
            to.setEditor("system");
            to.setEditTime(LocalDateTime.now());

            if (resp != null && resp.getStatusCode().is3xxRedirection()) {
                to.setClickStatus(ClickStatusEnum.UNCONVERTED.getStatus());
                getBaseMapper().updateByIdWithDate(to, date);
                return resp.getHeaders().getLocation();
            } else {
                to.setClickStatus(ClickStatusEnum.DISCARDED.getStatus());
                getBaseMapper().updateByIdWithDate(to, date);
                return null;
            }
        }

        throw new RuntimeException("unknowns error");
    }

    private ResponseEntity<String> requestTraceUri(String func, UriComponents adUri, Advertisement ad) {
            //识别uri不是指向本机
        if (IPUtils.isLocalhost(adUri.getHost())) {
            log.error("requestTraceUri uri指向本机. adUri:{}", adUri);
            return null;
        }
        return doRequestTraceUri(func, adUri, 1, ad);
    }

    private ResponseEntity<String> doRequestTraceUri(String func, UriComponents adUri, int times, Advertisement ad) {
        try {

            int maxRetryTimes = 2;
            if (times > maxRetryTimes) {
                log.warn("requestTraceUri retry times > {}", maxRetryTimes);
                return null;
            }

            //log.info("{} uri:{}", func ,adUri);
            ResponseEntity<String> resp = restTemplate.getForEntity(adUri.encode().toUri(), String.class);
            log.info("{} uri:{} resp:{}", func, adUri, resp);
            return resp;

        } catch (Exception e) {

            log.warn("requestTraceUri fail. func:{} adUri:{} err:{}", func, adUri, e.getMessage(), e);

            Throwable rootCause = NestedExceptionUtils.getRootCause(e);
            if (rootCause instanceof SocketTimeoutException) {
                log.warn("requestTraceUri fail. socket timeout. func:{} adId:{} company:{} adUri:{} err:{}",
                        func, ad.getId(), ad.getCompanyId(), adUri, rootCause.getMessage());
                redisUtils.execute(redis -> {
                    String key = String.format(Const.CLICK_SOCKET_TIME_OUT_MINUTE, LocalDateTime.now().format(DateUtils.yyyyMMdd_HHmm));
                    long count = redis.opsForHash().increment(key, ad.getCompanyId().toString(), 1);
                    redis.expire(key, 7, TimeUnit.DAYS);
                    int threshold = 50;
                    if (count > threshold) {
                        String content = String.format("广告主: %d. 1分钟内调用超时超过%d次, 请注意", ad.getCompanyId(), threshold);
                        WeChatRobotMsg robotMsg = WeChatRobotMsg.buildText().content(content).build();
                        weChatRobotService.notify(Const.ERROR_WEB_HOOK, robotMsg);
                    }
                });
                return null;
            }
            //if (rootCause instanceof HttpServerErrorException) {
            //    log.warn("requestTraceUri fail. server error. func:{} adUri:{} err:{}", func, adUri, rootCause.getMessage());
            //    return null;
            //}

            return null;
        }
    }

    private UriComponents buildAdTraceUri(String clickId, Advertisement ad, String date, ClickRecord clickRecord) {
        JSONObject paramJson = JSON.parseObject(clickRecord.getParamJson());
        String callback = Const.CALLBACK;
        UriComponentsBuilder adUriBuilder = UriComponentsBuilder.fromHttpUrl(ad.getTraceUrl());
        UriComponents traceUri = adUriBuilder.build();
        traceUri.getQueryParams().forEach((key, list) -> {
            if (!CollectionUtils.isEmpty(list)) {
                String value = list.get(0);
                if (StringUtils.hasLength(value) && Const.PARAM_PATTERN.matcher(value).matches()) {
                    if (value.toLowerCase().contains(callback)) {
                        UriComponents callbackUri = UriComponentsBuilder.newInstance()
                                .scheme(scheme)
                                .host(domain)
                                .path("callback")
                                .queryParam("date", date)
                                .queryParam("clickId", clickId)
                                .build();
                        adUriBuilder.replaceQueryParam(key, Arrays.asList(callbackUri.toUriString()));
                    } else {
                        adUriBuilder.replaceQueryParam(key, Arrays.asList(Optional.ofNullable(paramJson.getString(key)).orElse("")));
                    }
                }
            }
        });
        return adUriBuilder.build();
    }

    @Override
    @Transactional
    public void createTable() {
        IntStream.range(-1, createDays).forEach(day -> {
            String date = LocalDateTime.now().plusDays(day).format(DateUtils.yyyyMMdd);
            getBaseMapper().createTable(date);
        });
    }

    @Override
    @Transactional
    public void deleteClickTable() {
        LocalDateTime deleteDay = LocalDateTime.now().plusDays(-deleteDaysAgo);
        for (int i = 0; i < 30; i++) {
            String date = deleteDay.plusDays(-i).format(DateUtils.yyyyMMdd);
            getBaseMapper().deleteTable(date);
        }
    }
}
