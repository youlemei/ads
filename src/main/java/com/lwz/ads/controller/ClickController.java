package com.lwz.ads.controller;

import com.lwz.ads.bean.Response;
import com.lwz.ads.constant.PromoteStatusEnum;
import com.lwz.ads.constant.TraceTypeEnum;
import com.lwz.ads.entity.Advertisement;
import com.lwz.ads.entity.Channel;
import com.lwz.ads.entity.ClickRecord;
import com.lwz.ads.entity.PromoteRecord;
import com.lwz.ads.service.IAdvertisementService;
import com.lwz.ads.service.IChannelService;
import com.lwz.ads.service.IClickRecordService;
import com.lwz.ads.service.IPromoteRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Controller
public class ClickController {

    @Autowired
    private IClickRecordService clickRecordService;

    @Autowired
    private IPromoteRecordService promoteRecordService;

    @Autowired
    private IAdvertisementService advertisementService;

    @Autowired
    private IChannelService channelService;

    /**
     * 广告点击入口, 核心接口.
     * 日调用量100W以上
     *
     * @param adId
     * @param channelId
     * @param request
     * @return
     */
    @GetMapping("/click")
    public ResponseEntity<Response> click(@RequestParam Long adId, @RequestParam Long channelId,
                                          @RequestParam String type, @RequestParam Map<String, Object> request){
        try {
            log.info("click adId:{} channelId:{} request:{}", adId, channelId, request);

            //检查
            TraceTypeEnum traceType = TraceTypeEnum.valueOfType(type);
            if (adId <= 0 || channelId <= 0 || traceType == null) {
                log.warn("click invalid param. adId:{} channel:{}", adId, channelId);
                return ResponseEntity.badRequest().build();
            }
            //TODO: 缓存
            PromoteRecord promoteRecord = promoteRecordService.lambdaQuery()
                    .eq(PromoteRecord::getAdId, adId).eq(PromoteRecord::getChannelId, channelId).one();
            if (promoteRecord == null) {
                log.warn("click fail. 广告投放记录不存在. adId:{} channel:{}", adId, channelId);
                return ResponseEntity.badRequest().build();
            }
            Advertisement ad = advertisementService.getById(adId);
            TraceTypeEnum adTraceType = TraceTypeEnum.valueOfType(ad.getTraceType());
            if (adTraceType == TraceTypeEnum.REDIRECT && traceType == TraceTypeEnum.ASYNC) {
                log.warn("click fail. 暂不支持302转异步. adId:{} channel:{}", adId, channelId);
                return ResponseEntity.badRequest().build();
            }
            Channel channel = channelService.getById(channelId);
            LocalDateTime clickTime = LocalDateTime.now();
            PromoteStatusEnum promoteStatus = PromoteStatusEnum.valueOfStatus(promoteRecord.getPromoteStatus());
            if (!ad.getTraceStatus() || clickTime.isAfter(ad.getEndTime()) || promoteStatus != PromoteStatusEnum.RUNNING) {
                log.info("click adId:{} channelId:{} type:{} 已停止推广", adId, channelId, type);
                return ResponseEntity.badRequest().build();
            }

            //保存点击记录
            ClickRecord clickRecord = clickRecordService.saveClick(clickTime, request, type, promoteRecord, ad, channel);

            //TODO: 处理器
            switch (traceType) {
                case ASYNC:
                    clickRecordService.asyncHandleClick(clickRecord, ad);
                    log.info("click asyncHandleClick ok. adId:{} channelId:{}", adId, channelId);
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Response.success());
                case REDIRECT:
                    //302
                    URI uri = clickRecordService.redirectHandleClick(clickRecord, ad);
                    if (uri != null) {
                        log.info("click redirectHandleClick ok. adId:{} channelId:{} uri:{}", adId, channelId, uri);
                        return ResponseEntity.status(HttpStatus.FOUND).location(uri).build();
                    } else {
                        log.warn("click redirectHandleClick fail. uri is null adId:{} channelId:{}", adId, channelId);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                default:
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

        } catch (Exception e) {
            log.error("click fail. request:{} err:{}", request, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
