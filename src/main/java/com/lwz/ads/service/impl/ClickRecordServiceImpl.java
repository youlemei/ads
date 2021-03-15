package com.lwz.ads.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwz.ads.bean.DingTalkRobotMsg;
import com.lwz.ads.constant.ClickStatusEnum;
import com.lwz.ads.constant.Const;
import com.lwz.ads.constant.TraceTypeEnum;
import com.lwz.ads.mapper.ClickRecordMapper;
import com.lwz.ads.mapper.entity.Advertisement;
import com.lwz.ads.mapper.entity.ClickRecord;
import com.lwz.ads.mapper.entity.PromoteRecord;
import com.lwz.ads.service.*;
import com.lwz.ads.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.task.TaskDecorator;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
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

    @Autowired
    private DingTalkRobotService dingTalkRobotService;

    @Autowired
    private AdvertisementServiceImpl advertisementService;

    @Autowired
    private SysConfigLoader sysConfigLoader;

    @Autowired
    private TaskDecorator taskDecorator;

    @Autowired
    private RejectedExecutionHandler smartRejectedExecutionHandler;

    @Value("${system.web.scheme:http}")
    private String scheme;

    @Value("${system.web.domain:localhost:9999}")
    private String domain;

    private final ConcurrentMap<Long, ThreadPoolTaskExecutor> executorConcurrentMap = new ConcurrentHashMap<>();

    private ThreadPoolTaskExecutor retryExecutor;

    @PostConstruct
    public void init() {
        retryExecutor = new ThreadPoolTaskExecutor();
        retryExecutor.setCorePoolSize(100);
        retryExecutor.setMaxPoolSize(100);
        retryExecutor.setTaskDecorator(taskDecorator);
        retryExecutor.setQueueCapacity(1000);
        retryExecutor.setThreadNamePrefix("retry-");
        retryExecutor.setRejectedExecutionHandler(smartRejectedExecutionHandler);
        retryExecutor.initialize();
    }

    @PreDestroy
    public void destroy() {
        retryExecutor.shutdown();
        executorConcurrentMap.values().forEach(ExecutorConfigurationSupport::shutdown);
    }

    public ConcurrentMap<Long, ThreadPoolTaskExecutor> getExecutorConcurrentMap() {
        return executorConcurrentMap;
    }

    public ThreadPoolTaskExecutor getRetryExecutor() {
        return retryExecutor;
    }

    @Override
    //@Transactional
    public ClickRecord saveClick(LocalDateTime clickTime, Map<String, Object> request, String type, PromoteRecord promoteRecord, Advertisement ad) {
        Clock clock = new Clock();
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
                    if (placeHolder.contains(ip)) {
                        Object ipParam = Optional.ofNullable(request.get(key)).orElseGet(() -> request.get(ip));
                        if (ipParam != null) {
                            clickRecord.setIp(ipParam.toString());
                        }
                    }
                    if (placeHolder.contains(idfa) || placeHolder.contains(imei)) {
                        Object idfaParam = Optional.ofNullable(request.get(key)).orElseGet(() ->
                                Optional.ofNullable(request.get(idfa)).orElseGet(() -> request.get(imei)));
                        if (idfaParam != null) {
                            clickRecord.setMac(idfaParam.toString());
                        }
                    }
                }
            }
        });

        String date = clickTime.format(DateUtils.yyyyMMdd);
        getBaseMapper().insertWithDate(clickRecord, date);
        clock.tag();

        redisUtils.execute(redis -> {

            redis.executePipelined((RedisCallback<?>) connection -> {

                if (promoteRecord.getClickDayLimit() != null && promoteRecord.getClickDayLimit() > 0) {
                    String limitKey = String.format(Const.CLICK_DAY_LIMIT_KEY, date, promoteRecord.getId());
                    connection.incr(limitKey.getBytes());
                    connection.expire(limitKey.getBytes(), 7 * 86400);
                }

                if (ad.getClickDayLimit() != null && ad.getClickDayLimit() > 0) {
                    String adLimitKey = String.format(Const.AD_CLICK_DAY_LIMIT_KEY, date, ad.getId());
                    connection.incr(adLimitKey.getBytes());
                    connection.expire(adLimitKey.getBytes(), 7 * 86400);
                }

                String amountKey = String.format(Const.CLICK_DAY_AMOUNT, date);
                String pid = promoteRecord.getAdId() + "_" + promoteRecord.getChannelId();
                connection.hIncrBy(amountKey.getBytes(), pid.getBytes(), 1);
                connection.expire(amountKey.getBytes(), 7 * 86400);

                String actualKey = String.format(Const.CLICK_DAY_ACTUAL_AMOUNT, date, pid);
                int hashCode = (clickRecord.getIp() + clickRecord.getMac()).hashCode();
                connection.sAdd(actualKey.getBytes(), String.valueOf(hashCode).getBytes());
                connection.expire(actualKey.getBytes(), 86400);

                return null;
            });
        });

        log.info("saveClick adId:{} channelId:{} {}", ad.getId(), promoteRecord.getChannelId(), clock.tag());
        return clickRecord;
    }

    //@Async
    @Override
    //@Transactional(propagation = Propagation.REQUIRES_NEW)
    public void asyncHandleClick(ClickRecord clickRecord, Advertisement ad) {
        ThreadPoolTaskExecutor executor = executorConcurrentMap.computeIfAbsent(ad.getId(), adId -> {
            ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
            e.setCorePoolSize(20);
            e.setMaxPoolSize(50);
            e.setTaskDecorator(taskDecorator);
            e.setQueueCapacity(100);
            e.setThreadNamePrefix("ad-" + adId + "-");
            e.setRejectedExecutionHandler(smartRejectedExecutionHandler);
            e.initialize();
            return e;
        });
        executor.execute(() -> handleClick(clickRecord, ad));
    }

    @Override
    //@Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleClick(ClickRecord clickRecord, Advertisement ad) {

        if (clickRecord.getClickStatus().intValue() != ClickStatusEnum.RECEIVED.getStatus()) {
            return;
        }

        String date = clickRecord.getCreateTime().format(DateUtils.yyyyMMdd);

        //请求
        UriComponents adUri = buildAdTraceUri(clickRecord.getId(), ad, date, clickRecord);
        ResponseEntity<String> resp = requestTraceUri(adUri, ad, clickRecord);

        if (resp == null || resp.getStatusCode().is4xxClientError() || resp.getStatusCode().is5xxServerError()) {
            //增加重试次数
            getBaseMapper().incrRetryTimes(clickRecord.getId(), date);
            log.info("handleClick fail. adId:{} channelId:{} date:{} clickId:{}", ad.getId(), clickRecord.getChannelId(), date, clickRecord.getId());
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

        log.info("handleClick success. adId:{} channelId:{} date:{} clickId:{}",
                ad.getId(), clickRecord.getChannelId(), date, clickRecord.getId());
    }

    @Override
    //@Transactional
    public URI redirectHandleClick(ClickRecord clickRecord, Advertisement ad) {

        TraceTypeEnum traceType = TraceTypeEnum.valueOfType(ad.getTraceType());
        if (traceType == TraceTypeEnum.ASYNC) {
            SpringContextHolder.getBean(IClickRecordService.class).asyncHandleClick(clickRecord, ad);
            return URI.create(ad.getPreviewUrl());
        }

        if (traceType == TraceTypeEnum.REDIRECT) {

            String date = clickRecord.getCreateTime().format(DateUtils.yyyyMMdd);

            //请求
            UriComponents adUri = buildAdTraceUri(clickRecord.getId(), ad, date, clickRecord);
            ResponseEntity<String> resp = requestTraceUri(adUri, ad, clickRecord);

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

    private ResponseEntity<String> requestTraceUri(UriComponents adUri, Advertisement ad, ClickRecord clickRecord) {
        //识别uri不是指向本机
        if (IPUtils.isLocalhost(adUri.getHost())) {
            log.error("requestTraceUri uri指向本机. adId:{} channelId:{} adUri:{}", ad.getId(), clickRecord.getChannelId(), adUri);
            return null;
        }
        Clock clock = new Clock();
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(adUri.encode().toUri(), String.class);
            String body = resp.getBody();
            log.info("requestTraceUri success. adId:{} channelId:{} id:{} uri:{} code:{} body:{} {}",
                    ad.getId(), clickRecord.getChannelId(), clickRecord.getId(), adUri,
                    resp.getStatusCodeValue(), body != null ? body.substring(0, Math.min(100, body.length())) : null, clock.tag());
            return resp;

        } catch (Exception e) {

            log.warn("requestTraceUri fail. adId:{} channelId:{} {} adUri:{} err:{}",
                    ad.getId(), clickRecord.getChannelId(), clock.tag(), adUri, e.getMessage(), e);

            Throwable rootCause = NestedExceptionUtils.getRootCause(e);
            if (rootCause instanceof SocketTimeoutException) {
                redisUtils.execute(redis -> {
                    String key = String.format(Const.CLICK_SOCKET_TIME_OUT_MINUTE, LocalDateTime.now().format(DateUtils.yyyyMMdd_HHmm));
                    long count = redis.opsForHash().increment(key, ad.getId().toString(), 1);
                    redis.expire(key, 2, TimeUnit.DAYS);
                    int threshold = 100;
                    if (count % threshold == 0) {
                        String content = String.format("时间: %s 广告主: %d 广告: %s 1分钟内调用超时达到%d次, 请注意. url: %s",
                                LocalDateTime.now().format(DateUtils.DEFAULT_FORMATTER),
                                ad.getCompanyId(), ad.getAdName(), count, ad.getTraceUrl());
                        //weChatRobotService.notify(Const.WECHAT_ROBOT_URL, WeChatRobotMsg.buildText().content(content).build());
                        dingTalkRobotService.notify(Const.DING_ROBOT_URL, DingTalkRobotMsg.buildText().content(content).build());
                    }
                });
                return null;
            }

            return null;
        }
    }

    private UriComponents buildAdTraceUri(String clickId, Advertisement ad, String date, ClickRecord clickRecord) {
        JSONObject paramJson = JSON.parseObject(clickRecord.getParamJson());
        String callback = Const.CALLBACK;
        String ip = Const.IP;
        String idfa = Const.IDFA;
        String imei = Const.IMEI;
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
                    }
                    else if (value.toLowerCase().contains(ip)) {
                        adUriBuilder.replaceQueryParam(key, Arrays.asList(Optional.ofNullable(paramJson.getString(key))
                                .orElseGet(() -> Optional.ofNullable(paramJson.getString(ip)).orElse(""))));
                    }
                    else if (value.toLowerCase().contains(idfa)) {
                        adUriBuilder.replaceQueryParam(key, Arrays.asList(Optional.ofNullable(paramJson.getString(key))
                                .orElseGet(() -> Optional.ofNullable(paramJson.getString(idfa)).orElse(""))));
                    }
                    else if (value.toLowerCase().contains(imei)) {
                        adUriBuilder.replaceQueryParam(key, Arrays.asList(Optional.ofNullable(paramJson.getString(key))
                                .orElseGet(() -> Optional.ofNullable(paramJson.getString(imei)).orElse(""))));
                    }
                    else if (value.toLowerCase().contains(Const.TS)) {
                        adUriBuilder.replaceQueryParam(key, Arrays.asList(Optional.ofNullable(paramJson.getString(key))
                                .orElseGet(() -> Optional.ofNullable(paramJson.getString(Const.TS))
                                        .orElse(String.valueOf(System.currentTimeMillis() / 1000)))));
                    }
                    else if (value.toLowerCase().contains(Const.TMS)) {
                        adUriBuilder.replaceQueryParam(key, Arrays.asList(Optional.ofNullable(paramJson.getString(key))
                                .orElseGet(() -> Optional.ofNullable(paramJson.getString(Const.TMS))
                                        .orElse(String.valueOf(System.currentTimeMillis())))));
                    }
                    else if (value.toLowerCase().contains(Const.DT)) {
                        adUriBuilder.replaceQueryParam(key, Arrays.asList(Optional.ofNullable(paramJson.getString(key))
                                .orElseGet(() -> Optional.ofNullable(paramJson.getString(Const.DT))
                                        .orElse(LocalDateTime.now().format(DateUtils.DEFAULT_FORMATTER)))));
                    }
                    else {
                        adUriBuilder.replaceQueryParam(key, Arrays.asList(Optional.ofNullable(paramJson.getString(key)).orElse("")));
                    }
                }
            }
        });
        return adUriBuilder.build();
    }

    @Override
    public void createTable() {
        int createDays = sysConfigLoader.getInt("click_record_create_days", 2);
        IntStream.range(-1, createDays).forEach(day -> {
            String date = LocalDateTime.now().plusDays(day).format(DateUtils.yyyyMMdd);
            getBaseMapper().createTable(date);
        });
    }

    @Override
    public void deleteClickTable() {
        int deleteDaysAgo = sysConfigLoader.getInt("click_record_delete_days_ago", 30);
        LocalDateTime deleteDay = LocalDateTime.now().plusDays(-deleteDaysAgo);
        int range = 100;
        for (int i = 0; i < range; i++) {
            String date = deleteDay.plusDays(-i).format(DateUtils.yyyyMMdd);
            getBaseMapper().deleteTable(date);
        }
    }

    @Override
    public void retryClick(String date) {

        int limit = 1000;
        while (true) {
            Clock clock = new Clock();
            List<ClickRecord> clickRecordList = getBaseMapper().selectReceiveClick(date, limit);
            CountDownLatch countDownLatch = new CountDownLatch(clickRecordList.size());
            for (ClickRecord clickRecord : clickRecordList) {
                retryExecutor.execute(()->{
                    try {
                        Advertisement ad = advertisementService.getById(clickRecord.getAdId());
                        TraceTypeEnum adTraceType = TraceTypeEnum.valueOfType(ad.getTraceType());
                        if (adTraceType == TraceTypeEnum.REDIRECT) {
                            //丢弃
                            ClickRecord to = new ClickRecord();
                            to.setClickStatus(ClickStatusEnum.DISCARDED.getStatus());
                            to.setEditor("system");
                            to.setEditTime(LocalDateTime.now());
                            getBaseMapper().updateByIdWithDate(to, date);
                        } else {
                            handleClick(clickRecord, ad);
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }
            try {
                countDownLatch.await();
                log.info("retryClick finish size:{} date:{} {}", clickRecordList.size(), date, clock.tag());
            } catch (InterruptedException e) {
                log.error("retryClick interrupt. err:{}", e.getMessage(), e);
            }

            if (clickRecordList.size() < limit) {
                break;
            }
        }
    }

}
