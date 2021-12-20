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
import com.lwz.ads.mapper.entity.Company;
import com.lwz.ads.mapper.entity.PromoteRecord;
import com.lwz.ads.service.DingTalkRobotService;
import com.lwz.ads.service.IClickRecordService;
import com.lwz.ads.service.SysConfigLoader;
import com.lwz.ads.service.WeChatRobotService;
import com.lwz.ads.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.task.TaskDecorator;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private CompanyServiceImpl companyService;

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

    private final ConcurrentMap<String, Expression> spelExpressionParserMap = new ConcurrentHashMap<>();

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
        ThreadPoolTaskExecutor executor = executorConcurrentMap.computeIfAbsent(ad.getCompanyId(), companyId -> {
            Company company = companyService.getById(companyId);
            JSONObject config = JSON.parseObject(company.getJsonData());
            int core = 20;
            int max = 40;
            int queue = 100;
            if (config != null) {
                core = config.getIntValue("core");
                max = config.getIntValue("max");
                queue = config.getIntValue("queue");
            }
            ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
            e.setCorePoolSize(core);
            e.setMaxPoolSize(max);
            e.setTaskDecorator(taskDecorator);
            e.setQueueCapacity(queue);
            e.setThreadNamePrefix("company-" + companyId + "-");
            e.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
            e.initialize();
            return e;
        });
        executor.execute(() -> handleClick(clickRecord, ad));
    }

    public boolean removeExecutor(Long id) {
        ThreadPoolTaskExecutor executor = executorConcurrentMap.remove(id);
        if (executor != null) {
            executor.shutdown();
            return true;
        }
        return false;
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
        LocalDateTime now = LocalDateTime.now();
        to.setEditTime(now);
        getBaseMapper().updateWithDate(me, to, date);

        redisUtils.execute(redis->{
            redis.executePipelined((RedisCallback<?>) connection -> {
                long cost = ChronoUnit.MILLIS.between(clickRecord.getCreateTime(), now);
                String range = inRange(cost);
                byte[] key = String.format(Const.CLICK_COST_TOTAL_STAT, date).getBytes();
                connection.hIncrBy(key, "total_count".getBytes(), 1);
                connection.hIncrBy(key, "total_cost".getBytes(), cost);
                connection.hIncrBy(key, range.getBytes(), 1);
                return null;
            });
        });

        log.info("handleClick success. adId:{} channelId:{} date:{} clickId:{}",
                ad.getId(), clickRecord.getChannelId(), date, clickRecord.getId());
    }

    private String inRange(long cost) {
        if (cost < 1) {
            return "count_0-1";
        } else if (cost < 10) {
            return "count_1-9";
        } else if (cost < 100) {
            return "count_10-99";
        } else if (cost < 1000) {
            return "count_100-999";
        } else if (cost < 10000) {
            return "count_1000-9999";
        } else {
            return "count_10000+";
        }
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
        Map<String, String> signKeys = new HashMap<>();
        UriComponentsBuilder adUriBuilder = UriComponentsBuilder.fromHttpUrl(ad.getTraceUrl());
        UriComponents traceUri = adUriBuilder.build();
        traceUri.getQueryParams().forEach((key, list) -> {
            if (!CollectionUtils.isEmpty(list)) {
                String value = list.get(0);
                if (StringUtils.hasLength(value) && Const.PARAM_PATTERN.matcher(value).matches()) {
                    value = value.toLowerCase();
                    if (value.contains(Const.CALLBACK)) {
                        UriComponents callbackUri = UriComponentsBuilder.newInstance()
                                .scheme(scheme)
                                .host(domain)
                                .path("callback")
                                .queryParam("date", date)
                                .queryParam("clickId", clickId)
                                .build();
                        adUriBuilder.replaceQueryParam(key, callbackUri.toUriString());
                    }
                    else if (value.contains(Const.IP)) {
                        adUriBuilder.replaceQueryParam(key, Optional.ofNullable(paramJson.getString(key))
                                .orElseGet(() -> Optional.ofNullable(paramJson.getString(Const.IP)).orElse("")));
                    }
                    else if (value.contains(Const.IDFA)) {
                        adUriBuilder.replaceQueryParam(key, Optional.ofNullable(paramJson.getString(key))
                                .orElseGet(() -> Optional.ofNullable(paramJson.getString(Const.IDFA)).orElse("")));
                    }
                    else if (value.contains(Const.IMEI)) {
                        adUriBuilder.replaceQueryParam(key, Optional.ofNullable(paramJson.getString(key))
                                .orElseGet(() -> Optional.ofNullable(paramJson.getString(Const.IMEI)).orElse("")));
                    }
                    else if (value.contains(Const.SIGN)) {
                        signKeys.put(key, value);
                    }
                    else if (value.contains(Const.TS)) {
                        adUriBuilder.replaceQueryParam(key, Optional.ofNullable(paramJson.getString(key))
                                .orElseGet(() -> Optional.ofNullable(paramJson.getString(Const.TS))
                                        .orElse(String.valueOf(System.currentTimeMillis() / 1000))));
                    }
                    else if (value.contains(Const.TMS)) {
                        adUriBuilder.replaceQueryParam(key, Optional.ofNullable(paramJson.getString(key))
                                .orElseGet(() -> Optional.ofNullable(paramJson.getString(Const.TMS))
                                        .orElse(String.valueOf(System.currentTimeMillis()))));
                    }
                    else if (value.contains(Const.DT)) {
                        adUriBuilder.replaceQueryParam(key, Optional.ofNullable(paramJson.getString(key))
                                .orElseGet(() -> Optional.ofNullable(paramJson.getString(Const.DT))
                                        .orElse(LocalDateTime.now().format(DateUtils.DEFAULT_FORMATTER))));
                    }
                    else {
                        adUriBuilder.replaceQueryParam(key, Optional.ofNullable(paramJson.getString(key)).orElse(""));
                    }
                }
            }
        });
        if (signKeys.size() > 0) {
            JSONObject jsonData = JSON.parseObject(ad.getJsonData());
            if (jsonData != null && jsonData.containsKey(Const.SIGN)) {
                StandardEvaluationContext context = new StandardEvaluationContext();
                UriComponents tempUri = adUriBuilder.build();
                MultiValueMap<String, String> queryParams = tempUri.getQueryParams();
                traceUri.getQueryParams().forEach((key, list) -> {
                    if (!CollectionUtils.isEmpty(list)) {
                        String value = list.get(0);
                        if (StringUtils.hasLength(value) && Const.PARAM_PATTERN.matcher(value).matches()) {
                            value = value.toLowerCase();
                            if (value.contains(Const.CALLBACK)) {
                                context.setVariable(Const.CALLBACK, queryParams.getFirst(key));
                            } else if (value.contains(Const.IP)) {
                                context.setVariable(Const.IP, queryParams.getFirst(key));
                            } else if (value.contains(Const.IDFA)) {
                                context.setVariable(Const.IDFA, queryParams.getFirst(key));
                            } else if (value.contains(Const.IMEI)) {
                                context.setVariable(Const.IMEI, queryParams.getFirst(key));
                            } else if (value.contains(Const.TS)) {
                                context.setVariable(Const.TS, queryParams.getFirst(key));
                            } else if (value.contains(Const.TMS)) {
                                context.setVariable(Const.TMS, queryParams.getFirst(key));
                            } else if (value.contains(Const.DT)) {
                                context.setVariable(Const.DT, queryParams.getFirst(key));
                            }
                        }
                    }
                });

                signKeys.forEach((key, value) -> {
                    String signScript = jsonData.getString(value);
                    Expression expression = spelExpressionParserMap.computeIfAbsent(signScript, s -> {
                        SpelExpressionParser parser = new SpelExpressionParser();
                        Expression parseExpression = parser.parseExpression(signScript);
                        return parseExpression;
                    });
                    Object sign = expression.getValue(context);
                    adUriBuilder.replaceQueryParam(key, sign);
                });
            }
        }
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

    public static void main(String[] args) {
        SpelExpressionParser parser = new SpelExpressionParser();
        Expression expression = parser.parseExpression(" T(org.springframework.util.DigestUtils).md5DigestAsHex((#idfa + '1' + #ts + #did + T(java.net.URLEncoder).encode(#callback, 'UTF-8') + 'e10adc3949ba59abbe56e057f20f883e').toLowerCase().getBytes()) ");
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("idfa", "78965433132");
        context.setVariable("ts", "1616506127906");
        context.setVariable("did", "9988778232323");
        context.setVariable("callback", "https://www.example.com?xxx=XXXX");

        Object sign = expression.getValue(context);
        System.out.println(sign);


        System.out.println(DigestUtils.md5DigestAsHex("78965433132116165061279069988778232323https%3A%2F%2Fwww.example.com%3Fxxx%3DXXXXe10adc3949ba59abbe56e057f20f883e".toLowerCase().getBytes()));
    }

    /*

    {"sign":"T(org.springframework.util.DigestUtils).md5DigestAsHex((#idfa + ''1'' + #tms + ''xiaotongdc0423'' + T(java.net.URLEncoder).encode(#callback, ''UTF-8'') + ''ed2effae92c9f7178742f76bd0a8481f'').toLowerCase().getBytes())"}


    */


}
