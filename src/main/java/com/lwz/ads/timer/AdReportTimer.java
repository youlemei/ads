package com.lwz.ads.timer;

import com.lwz.ads.service.IAdvertisementReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdReportTimer {

    @Autowired
    private IAdvertisementReportService advertisementReportService;

    @Scheduled(fixedDelay = 30000)
    public void work(){
        //每分钟统计每日点击
        //每分钟统计每日转化
        advertisementReportService.countAdReport();
    }

}
