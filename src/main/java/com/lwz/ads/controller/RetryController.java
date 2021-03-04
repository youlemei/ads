package com.lwz.ads.controller;

import com.lwz.ads.bean.Response;
import com.lwz.ads.service.impl.AdvertisementReportServiceImpl;
import com.lwz.ads.service.impl.ClickRecordServiceImpl;
import com.lwz.ads.service.impl.ConvertRecordServiceImpl;
import com.lwz.ads.util.DateUtils;
import com.lwz.ads.util.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author liweizhou 2020/6/17
 */
@Slf4j
@RestController
public class RetryController {

    @Autowired
    private ConvertRecordServiceImpl convertRecordService;

    @Autowired
    private AdvertisementReportServiceImpl advertisementReportService;

    @Autowired
    private ClickRecordServiceImpl clickRecordService;

    @RequestMapping("/retryClick")
    public Response retryClick(@RequestParam @DateTimeFormat(pattern = "yyyyMMdd") Date date, HttpServletRequest request) {
        String ip = IPUtils.getRealIp(request);
        if (!IPUtils.isLocalhost(ip)) {
            log.info("retryClick 403 ip:{}", ip);
            return Response.fail(403, "403");
        }

        LocalDateTime dateTime = DateUtils.toLocalDateTime(date);

        //重试
        clickRecordService.retryClick(dateTime.format(DateUtils.yyyyMMdd));

        //更新报表
        advertisementReportService.updateReportWithMySQL(dateTime.toLocalDate());

        return Response.success();
    }

    @RequestMapping("/retryConvert")
    public Response retryConvert(@RequestParam @DateTimeFormat(pattern = "yyyyMMdd") Date date, HttpServletRequest request) {
        String ip = IPUtils.getRealIp(request);
        if (!IPUtils.isLocalhost(ip)) {
            log.info("retryConvert 403 ip:{}", ip);
            return Response.fail(403, "403");
        }

        LocalDateTime dateTime = DateUtils.toLocalDateTime(date);
        LocalDateTime start = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        //重试
        convertRecordService.retryConvert(start, end);

        //更新报表
        advertisementReportService.updateReportWithMySQL(dateTime.toLocalDate());

        return Response.success();
    }

}
