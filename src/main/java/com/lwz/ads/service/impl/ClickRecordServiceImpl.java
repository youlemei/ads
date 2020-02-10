package com.lwz.ads.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwz.ads.constant.ClickStatusEnum;
import com.lwz.ads.constant.Const;
import com.lwz.ads.constant.TraceTypeEnum;
import com.lwz.ads.entity.Advertisement;
import com.lwz.ads.entity.ClickRecord;
import com.lwz.ads.entity.PromoteRecord;
import com.lwz.ads.mapper.ClickRecordMapper;
import com.lwz.ads.service.IClickRecordService;
import com.lwz.ads.util.DateUtils;
import com.lwz.ads.util.RedisUtils;
import com.lwz.ads.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import java.net.URI;
import java.net.UnknownHostException;
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
    private AdvertisementServiceImpl advertisementService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisUtils redisUtils;

    @Value("${system.web.scheme:http}")
    private String scheme;

    @Value("${system.web.domain:localhost:9999}")
    private String domain;

    @Value("${click_record_create_days:2}")
    private Integer createDays;

    @Transactional
    @Override
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
        String ip = advertisementService.getJsonField(ad, Const.IP);
        String idfa = advertisementService.getJsonField(ad, Const.IDFA);
        String imei = advertisementService.getJsonField(ad, Const.IMEI);
        UriComponents adUri = UriComponentsBuilder.fromHttpUrl(ad.getTraceUrl()).build();
        adUri.getQueryParams().forEach((key, list) -> {
            if (!CollectionUtils.isEmpty(list)) {
                String value = list.get(0);
                if (StringUtils.hasLength(value) && Const.PARAM_PATTERN.matcher(value).matches()) {
                    String lowerCaseKey = key.toLowerCase();
                    if (lowerCaseKey.contains(ip) && request.containsKey(key)) {
                        Optional.ofNullable(request.get(key)).ifPresent(o -> clickRecord.setIp(o.toString()));
                    }
                    if ((lowerCaseKey.contains(idfa) || lowerCaseKey.contains(imei)) && request.containsKey(key)) {
                        Optional.ofNullable(request.get(key)).ifPresent(o -> clickRecord.setMac(o.toString()));
                    }
                }
            }
        });

        String date = clickTime.format(DateUtils.yyyyMMdd);
        getBaseMapper().insertWithDate(clickRecord, date);
        if (promoteRecord.getClickDayLimit() != null && promoteRecord.getClickDayLimit() > 0) {
            redisUtils.execute(redis -> {
                String key = String.format(Const.CLICK_DAY_LIMIT_KEY, date, promoteRecord.getId());
                redis.opsForValue().increment(key, 1);
                redis.expire(key, 7, TimeUnit.DAYS);
                return null;
            });
        }
        return clickRecord;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void asyncHandleClick(ClickRecord clickRecord, Advertisement ad) {

        if (clickRecord.getClickStatus().intValue() != ClickStatusEnum.RECEIVED.getStatus()) {
            return;
        }

        String date = clickRecord.getCreateTime().format(DateUtils.yyyyMMdd);
        UriComponents adUri = buildAdTraceUri(clickRecord.getId(), ad, date, clickRecord);
        ResponseEntity<String> resp = requestTraceUri("asyncHandleClick", adUri);

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

    @Transactional
    @Override
    public URI redirectHandleClick(ClickRecord clickRecord, Advertisement ad) {

        TraceTypeEnum traceType = TraceTypeEnum.valueOfType(ad.getTraceType());
        if (traceType == TraceTypeEnum.ASYNC) {
            SpringContextHolder.getBean(IClickRecordService.class).asyncHandleClick(clickRecord, ad);
            return URI.create(ad.getPreviewUrl());
        }

        if (traceType == TraceTypeEnum.REDIRECT) {

            String date = clickRecord.getCreateTime().format(DateUtils.yyyyMMdd);
            UriComponents adUri = buildAdTraceUri(clickRecord.getId(), ad, date, clickRecord);
            ResponseEntity<String> resp = requestTraceUri("redirectHandleClick", adUri);

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

    private ResponseEntity<String> requestTraceUri(String func, UriComponents adUri) {
        try {
            //识别uri不是指向本机
            if ("localhost".equals(adUri.getHost()) || "2020funfantasy.cn".equals(adUri.getHost()) || "47.107.70.137".equals(adUri.getHost())) {
                throw new UnknownHostException();
            }
            String uri = adUri.toUriString();
            log.info("{} uri:{}", func ,uri);
            ResponseEntity<String> resp = restTemplate.getForEntity(adUri.encode().toUri(), String.class);
            log.info("{} uri:{} resp:{}", func, uri, resp);
            return resp;
        } catch (Exception e) {
            log.error("requestTraceUri fail. func:{} adUri:{} err:{}", func, adUri, e.getMessage(), e);
            return null;
        }
    }

    private UriComponents buildAdTraceUri(String clickId, Advertisement ad, String date, ClickRecord clickRecord) {
        JSONObject paramJson = JSON.parseObject(clickRecord.getParamJson());
        String callback = advertisementService.getJsonField(ad, Const.CALLBACK);
        UriComponentsBuilder adUriBuilder = UriComponentsBuilder.fromHttpUrl(ad.getTraceUrl());
        UriComponents traceUri = adUriBuilder.build();
        traceUri.getQueryParams().forEach((key, list) -> {
            if (!CollectionUtils.isEmpty(list)) {
                String value = list.get(0);
                if (StringUtils.hasLength(value) && Const.PARAM_PATTERN.matcher(value).matches()) {
                    if (key.toLowerCase().contains(callback)) {
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

    @Transactional
    @Override
    public void createTable() {
        IntStream.range(0, createDays).forEach(day -> {
            String date = LocalDateTime.now().plusDays(day).format(DateUtils.yyyyMMdd);
            getBaseMapper().createTable(date);
        });
    }
}
