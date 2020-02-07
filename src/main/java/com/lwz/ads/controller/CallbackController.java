package com.lwz.ads.controller;

import com.lwz.ads.bean.Response;
import com.lwz.ads.constant.PromoteStatusEnum;
import com.lwz.ads.entity.Advertisement;
import com.lwz.ads.entity.Channel;
import com.lwz.ads.entity.ClickRecord;
import com.lwz.ads.entity.ConvertRecord;
import com.lwz.ads.entity.PromoteRecord;
import com.lwz.ads.mapper.ClickRecordMapper;
import com.lwz.ads.service.IAdvertisementService;
import com.lwz.ads.service.IChannelService;
import com.lwz.ads.service.IClickRecordService;
import com.lwz.ads.service.IConvertRecordService;
import com.lwz.ads.service.IPromoteRecordService;
import com.lwz.ads.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@RestController
public class CallbackController {

    @Autowired
    private IConvertRecordService convertRecordService;

    @Autowired
    private IClickRecordService clickRecordService;

    @Autowired
    private IPromoteRecordService promoteRecordService;

    @Autowired
    private IAdvertisementService advertisementService;

    @Autowired
    private IChannelService channelService;

    @GetMapping("/callback")
    public Response callback(@RequestParam String date, @RequestParam String clickId){
        try {
            log.info("callback date:{} clickId:{}", date, clickId);
            //检查
            LocalDateTime now = LocalDateTime.now();
            LocalDate localDate = LocalDate.parse(date, DateUtils.yyyyMMdd);
            if (date.length() != 8 || clickId.length() != 32 || localDate.atStartOfDay().isAfter(now)) {
                log.warn("callback invalid param. date:{} clickId:{}", date, clickId);
                return Response.with(HttpStatus.BAD_REQUEST);
            }
            ClickRecord clickRecord = ((ClickRecordMapper) clickRecordService.getBaseMapper()).selectByIdWithDate(clickId, date);
            if (clickRecord == null) {
                log.warn("callback fail. 点击记录不存在 date:{} clickId:{}", date, clickId);
                return Response.with(HttpStatus.BAD_REQUEST);
            }
            Advertisement ad = advertisementService.getById(clickRecord.getAdId());
            Channel channel = channelService.getById(clickRecord.getChannelId());
            PromoteRecord promoteRecord = promoteRecordService.lambdaQuery()
                    .eq(PromoteRecord::getAdId, clickRecord.getAdId()).eq(PromoteRecord::getChannelId, clickRecord.getChannelId()).one();
            PromoteStatusEnum promoteStatus = PromoteStatusEnum.valueOfStatus(promoteRecord.getPromoteStatus());
            if (!ad.getTraceStatus() || now.isAfter(ad.getEndTime()) || promoteStatus != PromoteStatusEnum.RUNNING) {
                log.info("callback date:{} clickId:{} 已停止推广", date, clickId);
                return Response.fail(400, "已停止推广");
            }

            //保存转化记录, 核减
            ConvertRecord convertRecord = convertRecordService.saveConvert(clickRecord, promoteRecord);
            if (convertRecord != null) {
                //异步回调渠道
                convertRecordService.asyncNotifyConvert(convertRecord);
            }

            log.info("callback success. date:{} clickId:{}", date, clickId);
            return Response.success();
        } catch (DuplicateKeyException e) {
            log.info("callback duplicate date:{} clickId:{}", date, clickId);
            return Response.success();
        } catch (Exception e) {
            log.error("callback fail. date:{} clickId:{} err:{}", date, clickId, e.getMessage(), e);
            return Response.with(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
