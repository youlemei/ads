package com.lwz.ads.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwz.ads.constant.ClickStatusEnum;
import com.lwz.ads.constant.Const;
import com.lwz.ads.constant.ConvertStatusEnum;
import com.lwz.ads.constant.PromoteStatusEnum;
import com.lwz.ads.mapper.ConvertRecordMapper;
import com.lwz.ads.mapper.entity.Advertisement;
import com.lwz.ads.mapper.entity.ClickRecord;
import com.lwz.ads.mapper.entity.ConvertRecord;
import com.lwz.ads.mapper.entity.PromoteRecord;
import com.lwz.ads.service.IConvertRecordService;
import com.lwz.ads.util.DateUtils;
import com.lwz.ads.util.IPUtils;
import com.lwz.ads.util.RedisUtils;
import com.lwz.ads.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 转化记录 服务实现类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
@Slf4j
@Service
public class ConvertRecordServiceImpl extends ServiceImpl<ConvertRecordMapper, ConvertRecord> implements IConvertRecordService {

    private final Random random = new Random();

    @Autowired
    private ClickRecordServiceImpl clickRecordService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisUtils redisUtils;

    @Override
    @Transactional
    public ConvertRecord saveConvert(ClickRecord clickRecord, PromoteRecord promoteRecord, Advertisement ad, String date) {
        if (lambdaQuery().eq(ConvertRecord::getClickId, clickRecord.getId()).count() > 0) {
            return null;
        }

        JSONObject paramJson = JSON.parseObject(clickRecord.getParamJson());
        JSONObject jsonData = new JSONObject();
        jsonData.put(Const.IP, clickRecord.getIp());
        jsonData.put(Const.MAC, clickRecord.getMac());
        LocalDateTime now = LocalDateTime.now();
        String today = now.format(DateUtils.yyyyMMdd);
        ConvertRecord convertRecord = new ConvertRecord()
                .setClickId(clickRecord.getId())
                .setClickTime(clickRecord.getCreateTime())
                .setCreateTime(now)
                .setAdId(promoteRecord.getAdId())
                .setAdCreator(promoteRecord.getAdCreator())
                .setChannelId(promoteRecord.getChannelId())
                .setChannelCreator(promoteRecord.getChannelCreator())
                .setCallback(paramJson.getString(Const.CALLBACK))
                .setJsonData(jsonData.toJSONString());
        redisUtils.execute(redis -> {
            String key = String.format(Const.CONVERT_DAY_AMOUNT, today);
            redis.opsForHash().increment(key, promoteRecord.getAdId() + "_" + promoteRecord.getChannelId(), 1);
            redis.expire(key, 7, TimeUnit.DAYS);
        });

        //核减
        boolean deduct = isDeduct(promoteRecord, ad, today);
        if (deduct) {
            convertRecord.setConvertStatus(ConvertStatusEnum.DEDUCTED.getStatus());
            save(convertRecord);
            ClickRecord to = new ClickRecord();
            to.setId(clickRecord.getId());
            to.setEditor("system");
            to.setEditTime(now);
            to.setClickStatus(ClickStatusEnum.DEDUCTED.getStatus());
            clickRecordService.getBaseMapper().updateByIdWithDate(to, date);
            return null;
        }

        //转化
        convertRecord.setConvertStatus(ConvertStatusEnum.CONVERTED.getStatus());
        save(convertRecord);
        if (promoteRecord.getConvertDayLimit() != null && promoteRecord.getConvertDayLimit() > 0) {
            redisUtils.execute(redis -> {
                String limitKey = String.format(Const.CONVERT_DAY_LIMIT_KEY, today, promoteRecord.getId());
                redis.opsForValue().increment(limitKey, 1);
                redis.expire(limitKey, 7, TimeUnit.DAYS);
            });
        }
        if (ad.getConvertDayLimit() != null && ad.getConvertDayLimit() > 0) {
            redisUtils.execute(redis -> {
                String adLimitKey = String.format(Const.AD_CONVERT_DAY_LIMIT_KEY, today, ad.getId());
                redis.opsForValue().increment(adLimitKey, 1);
                redis.expire(adLimitKey, 7, TimeUnit.DAYS);
            });
        }
        redisUtils.execute(redis -> {
            String amountKey = String.format(Const.CONVERT_DAY_ACTUAL_AMOUNT, today);
            redis.opsForHash().increment(amountKey, promoteRecord.getAdId() + "_" + promoteRecord.getChannelId(), 1);
            redis.expire(amountKey, 7, TimeUnit.DAYS);
        });
        return convertRecord;
    }

    private boolean isDeduct(PromoteRecord promoteRecord, Advertisement ad, String today) {

        if (promoteRecord.getPromoteStatus().intValue() != PromoteStatusEnum.RUNNING.getStatus()) {
            log.info("deduct. pid:{} 已停止推广", promoteRecord.getId());
            return true;
        }

        Integer convertDayLimit = promoteRecord.getConvertDayLimit();
        if (convertDayLimit != null && convertDayLimit > 0) {
            Integer dayConvert = redisUtils.get(String.format(Const.CONVERT_DAY_LIMIT_KEY, today, promoteRecord.getId()), Integer.class);
            if (dayConvert != null && dayConvert >= convertDayLimit) {
                log.info("deduct. pid:{} dayConvert:{} limit:{} 转化已超过每日上限", promoteRecord.getId(), dayConvert, convertDayLimit);
                return true;
            }
        }

        Integer adConvertDayLimit = ad.getConvertDayLimit();
        if (adConvertDayLimit != null && adConvertDayLimit > 0) {
            Integer adDayConvert = redisUtils.get(String.format(Const.AD_CONVERT_DAY_LIMIT_KEY, today, ad.getId()), Integer.class);
            if (adDayConvert != null && adDayConvert >= adConvertDayLimit) {
                log.info("deduct. adId:{} dayConvert:{} adLimit:{} 转化已超过每日上限", ad.getId(), adDayConvert, adConvertDayLimit);
                return true;
            }
        }

        if (promoteRecord.getDeductRate() != null && promoteRecord.getDeductRate() > 0) {
            int index = random.nextInt(100);
            log.info("saveConvert pid:{} deduct:{} index:{}", promoteRecord.getId(), promoteRecord.getDeductRate(), index);
            return index < promoteRecord.getDeductRate();
        }

        return false;
    }

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void asyncNotifyConvert(ConvertRecord convertRecord) {
        notifyConvert(convertRecord);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyConvert(ConvertRecord convertRecord) {

        if (convertRecord.getConvertStatus().intValue() != ConvertStatusEnum.CONVERTED.getStatus()) {
            return;
        }

        String callback = convertRecord.getCallback();
        if (StringUtils.hasText(callback)) {

            //调用渠道转化链接
            ResponseEntity<String> resp = callbackConvert(callback, convertRecord);

            if (resp == null || !resp.getStatusCode().is2xxSuccessful()) {
                getBaseMapper().incrRetryTimes(convertRecord.getClickId());
                return;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        ConvertRecord to = new ConvertRecord();
        to.setEditor("system");
        to.setEditTime(now);
        to.setConvertStatus(ConvertStatusEnum.NOTIFIED.getStatus());
        update().eq("click_id", convertRecord.getClickId())
                .eq("convert_status", ConvertStatusEnum.CONVERTED.getStatus())
                .update(to);

        ClickRecord clickTo = new ClickRecord();
        clickTo.setId(convertRecord.getClickId());
        clickTo.setEditor("system");
        clickTo.setEditTime(now);
        clickTo.setClickStatus(ClickStatusEnum.CONVERTED.getStatus());
        clickRecordService.getBaseMapper().updateByIdWithDate(clickTo, convertRecord.getClickTime().format(DateUtils.yyyyMMdd));

        log.info("notifyConvert success. adId:{} channelId:{} clickId:{}",
                convertRecord.getAdId(), convertRecord.getChannelId(), convertRecord.getClickId());
    }

    private ResponseEntity<String> callbackConvert(String callback, ConvertRecord convertRecord) {
        try {
            //识别callback不是指向本机
            UriComponents uri = UriComponentsBuilder.fromHttpUrl(StringUtils.trimWhitespace(callback)).build();
            if (IPUtils.isLocalhost(uri.getHost())) {
                throw new UnknownHostException();
            }
            log.info("callbackConvert adId:{} channelId:{} callback:{}",
                    convertRecord.getAdId(), convertRecord.getChannelId(), callback);
            ResponseEntity<String> resp = restTemplate.getForEntity(uri.encode().toUri(), String.class);
            String body = resp.getBody();
            log.info("callbackConvert adId:{} channelId:{} callback:{} code:{} body:{}",
                    convertRecord.getAdId(), convertRecord.getChannelId(), callback,
                    resp.getStatusCodeValue(), body != null ? body.substring(0, Math.min(100, body.length())) : null);
            return resp;
        } catch (Exception e) {
            log.warn("callbackConvert fail. adId:{} channelId:{} callback:{} err:{}",
                    convertRecord.getAdId(), convertRecord.getChannelId(), callback, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void retryConvert(LocalDateTime start, LocalDateTime end) {
        ConvertRecordServiceImpl convertRecordService = SpringContextHolder.getBean(ConvertRecordServiceImpl.class);
        lambdaQuery()
                .between(ConvertRecord::getCreateTime, start, end)
                .eq(ConvertRecord::getConvertStatus, ConvertStatusEnum.CONVERTED.getStatus())
                .lt(ConvertRecord::getRetryTimes, 3)
                .list()
                .forEach(convertRecord -> convertRecordService.notifyConvert(convertRecord));
    }

}
