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

    @Transactional
    @Override
    public boolean saveConvert(ClickRecord clickRecord, PromoteRecord promoteRecord) {
        if (getById(clickRecord.getId()) != null) {
            return false;
        }

        JSONObject paramJson = JSON.parseObject(clickRecord.getParamJson());
        ConvertRecord convertRecord = new ConvertRecord()
                .setClickId(clickRecord.getId())
                .setClickTime(clickRecord.getCreateTime())
                .setCreateTime(LocalDateTime.now())
                .setAdId(promoteRecord.getAdId())
                .setAdCreator(promoteRecord.getAdCreator())
                .setChannelId(promoteRecord.getChannelId())
                .setChannelCreator(promoteRecord.getChannelCreator())
                .setCallback(paramJson.getString(Const.CALLBACK));

        if (promoteRecord.getDeductRate() != null && promoteRecord.getDeductRate() > 0) {
            int index = random.nextInt(100);
            if (index < promoteRecord.getDeductRate()) {
                //核减
                convertRecord.setConvertStatus(ConvertStatusEnum.DEDUCTED.getStatus());
                save(convertRecord);

                ClickRecord to = new ClickRecord();
                to.setId(clickRecord.getId());
                to.setEditor("system");
                to.setEditTime(LocalDateTime.now());
                to.setClickStatus(ClickStatusEnum.DEDUCTED.getStatus());
                clickRecordService.getBaseMapper().updateByIdWithDate(to, clickRecord.getCreateTime().format(DateUtils.yyyyMMdd));
                return false;
            }
        }
        //转化
        convertRecord.setConvertStatus(ConvertStatusEnum.CONVERTED.getStatus());
        save(convertRecord);
        return true;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void asyncNotifyConvert(String clickId) {
        ConvertRecord convertRecord = getById(clickId);
        if (convertRecord.getConvertStatus().intValue() != ConvertStatusEnum.CONVERTED.getStatus()) {
            return;
        }

        String callback = convertRecord.getCallback();
        if (StringUtils.hasText(callback)) {
            callbackConvert(callback);
        }

        LocalDateTime now = LocalDateTime.now();
        ConvertRecord to = new ConvertRecord();
        to.setEditor("system");
        to.setEditTime(now);
        to.setConvertStatus(ConvertStatusEnum.NOTIFIED.getStatus());
        if (update(to, update().eq("click_id", clickId).eq("convert_status", ConvertStatusEnum.CONVERTED.getStatus()))) {
            ClickRecord clickTo = new ClickRecord();
            clickTo.setId(clickId);
            clickTo.setEditor("system");
            clickTo.setEditTime(now);
            clickTo.setClickStatus(ClickStatusEnum.CONVERTED.getStatus());
            clickRecordService.getBaseMapper().updateByIdWithDate(clickTo, convertRecord.getClickTime().format(DateUtils.yyyyMMdd));
        }
    }

    private void callbackConvert(String callback) {
        doCallbackConvert(callback, 1);
    }

    private void doCallbackConvert(String callback, int times) {
        try {
            if (times > 2) {
                return;
            }
            log.info("doCallbackConvert callback:{}", callback);
            ResponseEntity<String> resp = restTemplate.getForEntity(StringUtils.trimWhitespace(callback), String.class);
            log.info("doCallbackConvert callback:{} resp:{}", callback, resp);
        } catch (RestClientException e) {
            log.error("doCallbackConvert fail. callback:{} times:{} err:{}", callback, times++, e.getMessage(), e);
            doCallbackConvert(callback, times);
        }
    }
}
