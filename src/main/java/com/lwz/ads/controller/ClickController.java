package com.lwz.ads.controller;

import com.lwz.ads.constant.Const;
import com.lwz.ads.constant.PromoteStatusEnum;
import com.lwz.ads.constant.TraceTypeEnum;
import com.lwz.ads.mapper.entity.Advertisement;
import com.lwz.ads.mapper.entity.ClickRecord;
import com.lwz.ads.mapper.entity.PromoteRecord;
import com.lwz.ads.service.impl.AdvertisementServiceImpl;
import com.lwz.ads.service.impl.ClickRecordServiceImpl;
import com.lwz.ads.service.impl.PromoteRecordServiceImpl;
import com.lwz.ads.util.DateUtils;
import com.lwz.ads.util.IPUtils;
import com.lwz.ads.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Controller
public class ClickController {

    @Autowired
    private ClickRecordServiceImpl clickRecordService;

    @Autowired
    private PromoteRecordServiceImpl promoteRecordService;

    @Autowired
    private AdvertisementServiceImpl advertisementService;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 广告点击入口, 核心接口.
     * 日调用量100W以上
     *
     * @param adId
     * @param channelId
     * @param type
     * @param request
     * @return
     */
    @GetMapping("/click")
    public ResponseEntity<String> click(HttpServletRequest httpServletRequest,
                                        @RequestParam Long adId, @RequestParam Long channelId,
                                        @RequestParam String type, @RequestParam Map<String, Object> request){
        try {
            log.info("click adId:{} channelId:{} request:{} ip:{}", adId, channelId, request, IPUtils.getRealIp(httpServletRequest));

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
                log.info("click fail. 暂不支持302转异步. adId:{} channel:{}", adId, channelId);
                return ResponseEntity.badRequest().body("暂不支持302转异步");
            }
            LocalDateTime clickTime = LocalDateTime.now();
            if (!ad.getTraceStatus() || clickTime.isAfter(ad.getEndTime())
                    || promoteRecord.getPromoteStatus().intValue() != PromoteStatusEnum.RUNNING.getStatus()) {
                log.info("click fail. adId:{} channelId:{} 已停止推广", adId, channelId);
                return ResponseEntity.badRequest().body("已停止推广");
            }
            if (promoteRecord.getClickDayLimit() != null && promoteRecord.getClickDayLimit() > 0) {
                String date = clickTime.format(DateUtils.yyyyMMdd);
                Integer dayClick = redisUtils.get(String.format(Const.CLICK_DAY_LIMIT_KEY, date, promoteRecord.getId()), Integer.class);
                if (dayClick != null && dayClick >= promoteRecord.getClickDayLimit()) {
                    log.info("click fail. adId:{} channelId:{} 点击已超过每日上限", adId, channelId);
                    return ResponseEntity.badRequest().body("点击已超过每日上限");
                }
            }
            if (promoteRecord.getConvertDayLimit() != null && promoteRecord.getConvertDayLimit() > 0) {
                String date = clickTime.format(DateUtils.yyyyMMdd);
                Integer dayConvert = redisUtils.get(String.format(Const.CONVERT_DAY_LIMIT_KEY, date, promoteRecord.getId()), Integer.class);
                if (dayConvert != null && dayConvert >= promoteRecord.getConvertDayLimit()) {
                    log.info("click fail. adId:{} channelId:{} 转化已超过每日上限", adId, channelId);
                    return ResponseEntity.badRequest().body("转化已超过每日上限");
                }
            }

            //保存点击记录
            ClickRecord clickRecord = clickRecordService.saveClick(clickTime, request, type, promoteRecord, ad);

            //TODO: 处理器
            switch (traceType) {
                case ASYNC:
                    clickRecordService.asyncHandleClick(clickRecord, ad);
                    log.info("click asyncHandleClick ok. adId:{} channelId:{}", adId, channelId);
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("OK");
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
