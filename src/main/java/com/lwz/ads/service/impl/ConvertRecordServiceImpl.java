package com.lwz.ads.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwz.ads.constant.ClickStatusEnum;
import com.lwz.ads.constant.Const;
import com.lwz.ads.constant.ConvertStatusEnum;
import com.lwz.ads.entity.ClickRecord;
import com.lwz.ads.entity.ConvertRecord;
import com.lwz.ads.entity.PromoteRecord;
import com.lwz.ads.mapper.ConvertRecordMapper;
import com.lwz.ads.service.IConvertRecordService;
import com.lwz.ads.util.DateUtils;
import com.lwz.ads.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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

    @Transactional
    @Override
    public ConvertRecord saveConvert(ClickRecord clickRecord, PromoteRecord promoteRecord, String date) {
        if (lambdaQuery().eq(ConvertRecord::getClickId, clickRecord.getId()).count() > 0) {
            return null;
        }

        JSONObject paramJson = JSON.parseObject(clickRecord.getParamJson());
        JSONObject jsonData = new JSONObject();
        jsonData.put(Const.IP, clickRecord.getIp());
        jsonData.put(Const.MAC, clickRecord.getMac());
        ConvertRecord convertRecord = new ConvertRecord()
                .setClickId(clickRecord.getId())
                .setClickTime(clickRecord.getCreateTime())
                .setCreateTime(LocalDateTime.now())
                .setAdId(promoteRecord.getAdId())
                .setAdCreator(promoteRecord.getAdCreator())
                .setChannelId(promoteRecord.getChannelId())
                .setChannelCreator(promoteRecord.getChannelCreator())
                .setCallback(paramJson.getString(Const.CALLBACK))
                .setJsonData(jsonData.toJSONString());

        if (promoteRecord.getDeductRate() != null && promoteRecord.getDeductRate() > 0) {
            int index = random.nextInt(100);
            log.info("saveConvert deduct:{} index:{}", promoteRecord.getDeductRate(), index);
            if (index < promoteRecord.getDeductRate()) {
                //核减
                convertRecord.setConvertStatus(ConvertStatusEnum.DEDUCTED.getStatus());
                save(convertRecord);

                ClickRecord to = new ClickRecord();
                to.setId(clickRecord.getId());
                to.setEditor("system");
                to.setEditTime(LocalDateTime.now());
                to.setClickStatus(ClickStatusEnum.DEDUCTED.getStatus());
                clickRecordService.getBaseMapper().updateByIdWithDate(to, date);
                return null;
            }
        }
        //转化
        convertRecord.setConvertStatus(ConvertStatusEnum.CONVERTED.getStatus());
        save(convertRecord);
        if (promoteRecord.getConvertDayLimit() != null && promoteRecord.getConvertDayLimit() > 0) {
            redisUtils.execute(redis -> {
                String key = String.format(Const.CONVERT_DAY_LIMIT_KEY, date, promoteRecord.getId());
                redis.opsForValue().increment(key, 1);
                redis.expire(key, 7, TimeUnit.DAYS);
                return null;
            });
        }
        return convertRecord;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void asyncNotifyConvert(ConvertRecord convertRecord) {

        if (convertRecord.getConvertStatus().intValue() != ConvertStatusEnum.CONVERTED.getStatus()) {
            return;
        }

        String callback = convertRecord.getCallback();
        if (StringUtils.hasText(callback)) {
            ResponseEntity<String> resp = callbackConvert(callback);
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
        update().eq("click_id", convertRecord.getClickId()).eq("convert_status", ConvertStatusEnum.CONVERTED.getStatus()).update(to);

        ClickRecord clickTo = new ClickRecord();
        clickTo.setId(convertRecord.getClickId());
        clickTo.setEditor("system");
        clickTo.setEditTime(now);
        clickTo.setClickStatus(ClickStatusEnum.CONVERTED.getStatus());
        clickRecordService.getBaseMapper().updateByIdWithDate(clickTo, convertRecord.getClickTime().format(DateUtils.yyyyMMdd));

        log.info("asyncNotifyConvert success. clickId:{}", convertRecord.getClickId());
    }

    private ResponseEntity<String> callbackConvert(String callback) {
        try {
            log.info("callbackConvert callback:{}", callback);
            ResponseEntity<String> resp = restTemplate.getForEntity(StringUtils.trimWhitespace(callback), String.class);
            log.info("callbackConvert callback:{} resp:{}", callback, resp);
            return resp;
        } catch (RestClientException e) {
            log.error("callbackConvert fail. callback:{} times:{} err:{}", callback, e.getMessage(), e);
            return null;
        }
    }

}
