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
import com.lwz.ads.util.Clock;
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
            Clock clock = new Clock();
            log.info("click adId:{} channelId:{} request:{} ip:{}", adId, channelId, request, IPUtils.getRealIp(httpServletRequest));

            //参数检查
            TraceTypeEnum traceType = TraceTypeEnum.valueOfType(type);
            if (adId <= 0 || channelId <= 0 || traceType == null) {
                log.warn("click invalid param. adId:{} channel:{}", adId, channelId);
                return ResponseEntity.badRequest().build();
            }

            LocalDateTime clickTime = LocalDateTime.now();
            Advertisement ad = advertisementService.getById(adId);
            PromoteRecord promoteRecord = promoteRecordService.lambdaQuery()
                    .eq(PromoteRecord::getAdId, adId).eq(PromoteRecord::getChannelId, channelId).one();

            //数据检查
            ResponseEntity result = check(clickTime, ad, promoteRecord, adId, channelId, traceType);
            if (result != null) {
                return result;
            }
            clock.tag();

            //保存点击记录
            ClickRecord clickRecord = clickRecordService.saveClick(clickTime, request, type, promoteRecord, ad);
            clock.tag();

            switch (traceType) {
                case ASYNC:
                    clickRecordService.asyncHandleClick(clickRecord, ad);
                    log.info("click asyncHandleClick ok. adId:{} channelId:{} {}", adId, channelId, clock.tag());
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

    private ResponseEntity check(LocalDateTime clickTime, Advertisement ad, PromoteRecord promoteRecord,
                                 Long adId, Long channelId, TraceTypeEnum traceType) {

        if (promoteRecord == null || ad == null) {
            log.warn("click fail. 广告投放记录不存在. adId:{} channel:{}", adId, channelId);
            return ResponseEntity.badRequest().build();
        }

        TraceTypeEnum adTraceType = TraceTypeEnum.valueOfType(ad.getTraceType());
        if (adTraceType == TraceTypeEnum.REDIRECT && traceType == TraceTypeEnum.ASYNC) {
            log.info("click fail. 暂不支持302转异步. adId:{} channel:{}", adId, channelId);
            return ResponseEntity.badRequest().body("暂不支持302转异步");
        }

        if (!ad.getTraceStatus() || clickTime.isAfter(ad.getEndTime())
                || promoteRecord.getPromoteStatus().intValue() != PromoteStatusEnum.RUNNING.getStatus()) {
            log.info("click fail. adId:{} channelId:{} 已停止推广", adId, channelId);
            return ResponseEntity.badRequest().body("已停止推广");
        }

        String date = clickTime.format(DateUtils.yyyyMMdd);
        Integer adClickLimit = ad.getClickDayLimit();
        if (adClickLimit != null && adClickLimit > 0) {
            Integer dayClick = redisUtils.get(String.format(Const.AD_CLICK_DAY_LIMIT_KEY, date, ad.getId()), Integer.class);
            if (dayClick != null && dayClick >= adClickLimit) {
                log.info("click fail. adId:{} channelId:{} 点击已超过每日上限 dayClick:{} adLimit:{}", adId, channelId, dayClick, adClickLimit);
                return ResponseEntity.badRequest().body("点击已超过每日上限");
            }
        }

        Integer adConvertLimit = ad.getConvertDayLimit();
        if (adConvertLimit != null && adConvertLimit > 0) {
            Integer dayConvert = redisUtils.get(String.format(Const.AD_CONVERT_DAY_LIMIT_KEY, date, ad.getId()), Integer.class);
            if (dayConvert != null && dayConvert >= adConvertLimit) {
                log.info("click fail. adId:{} channelId:{} 转化已超过每日上限 dayConvert:{} adLimit:{}", adId, channelId, dayConvert, adConvertLimit);
                return ResponseEntity.badRequest().body("转化已超过每日上限");
            }
        }

        Integer clickDayLimit = promoteRecord.getClickDayLimit();
        if (clickDayLimit != null && clickDayLimit > 0) {
            Integer dayClick = redisUtils.get(String.format(Const.CLICK_DAY_LIMIT_KEY, date, promoteRecord.getId()), Integer.class);
            if (dayClick != null && dayClick >= clickDayLimit) {
                log.info("click fail. adId:{} channelId:{} 点击已超过每日上限 dayClick:{} limit:{}", adId, channelId, dayClick, clickDayLimit);
                return ResponseEntity.badRequest().body("点击已超过每日上限");
            }
        }

        Integer convertDayLimit = promoteRecord.getConvertDayLimit();
        if (convertDayLimit != null && convertDayLimit > 0) {
            Integer dayConvert = redisUtils.get(String.format(Const.CONVERT_DAY_LIMIT_KEY, date, promoteRecord.getId()), Integer.class);
            if (dayConvert != null && dayConvert >= convertDayLimit) {
                log.info("click fail. adId:{} channelId:{} 转化已超过每日上限 dayConvert:{} limit:{}", adId, channelId, dayConvert, convertDayLimit);
                return ResponseEntity.badRequest().body("转化已超过每日上限");
            }
        }

        return null;
    }

}
