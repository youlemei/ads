package com.lwz.ads.controller;

import com.lwz.ads.bean.Response;
import com.lwz.ads.mapper.entity.Advertisement;
import com.lwz.ads.mapper.entity.ClickRecord;
import com.lwz.ads.mapper.entity.ConvertRecord;
import com.lwz.ads.mapper.entity.PromoteRecord;
import com.lwz.ads.service.impl.AdvertisementServiceImpl;
import com.lwz.ads.service.impl.ClickRecordServiceImpl;
import com.lwz.ads.service.impl.ConvertRecordServiceImpl;
import com.lwz.ads.service.impl.PromoteRecordServiceImpl;
import com.lwz.ads.util.Clock;
import com.lwz.ads.util.DateUtils;
import com.lwz.ads.util.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@RestController
public class CallbackController {

    @Autowired
    private ConvertRecordServiceImpl convertRecordService;

    @Autowired
    private ClickRecordServiceImpl clickRecordService;

    @Autowired
    private PromoteRecordServiceImpl promoteRecordService;

    @Autowired
    private AdvertisementServiceImpl advertisementService;

    /**
     * 广告转化回调
     *
     * @param date
     * @param clickId
     * @return
     */
    @GetMapping("/callback")
    public Response callback(HttpServletRequest httpServletRequest,
                             @RequestParam String date, @RequestParam String clickId){
        try {
            Clock clock = new Clock();
            log.info("callback date:{} clickId:{} ip:{}", date, clickId, IPUtils.getRealIp(httpServletRequest));

            //检查
            LocalDateTime now = LocalDateTime.now();
            LocalDate localDate = LocalDate.parse(date, DateUtils.yyyyMMdd);
            if (date.length() != 8 || clickId.length() != 32 || localDate.atStartOfDay().isAfter(now)) {
                log.warn("callback invalid param. date:{} clickId:{}", date, clickId);
                return Response.with(HttpStatus.BAD_REQUEST);
            }

            ClickRecord clickRecord = clickRecordService.getBaseMapper().selectByIdWithDate(clickId, date);
            if (clickRecord == null) {
                log.warn("callback fail. 点击记录不存在 date:{} clickId:{}", date, clickId);
                return Response.with(HttpStatus.BAD_REQUEST);
            }

            Advertisement ad = advertisementService.getById(clickRecord.getAdId());
            PromoteRecord promoteRecord = promoteRecordService.lambdaQuery()
                    .eq(PromoteRecord::getAdId, clickRecord.getAdId()).eq(PromoteRecord::getChannelId, clickRecord.getChannelId()).one();
            clock.tag();

            //保存转化记录, 核减
            ConvertRecord convertRecord = convertRecordService.saveConvert(clickRecord, promoteRecord, ad, date);
            clock.tag();
            if (convertRecord != null) {
                //异步回调渠道
                convertRecordService.asyncNotifyConvert(convertRecord);
            }

            log.info("callback success. date:{} clickId:{} adId:{} channelId:{} {}", date, clickId,
                    clickRecord.getAdId(), clickRecord.getChannelId(), clock.tag());
            return Response.success();
        } catch (DuplicateKeyException e) {
            log.info("callback duplicate. date:{} clickId:{}", date, clickId);
            return Response.success();
        } catch (BadSqlGrammarException e) {
            log.info("callback click_record is deleted. date:{} clickId:{} err:{}", date, clickId, e.getMessage());
            return Response.success();
        } catch (Exception e) {
            log.error("callback fail. date:{} clickId:{} err:{}", date, clickId, e.getMessage(), e);
            return Response.with(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
