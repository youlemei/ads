package com.lwz.ads.timer;

import com.lwz.ads.service.IAdvertisementReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 触发式更新到redis/定时同步到报表 + 次日计算报表
 */
@Component
public class AdReportTimer {

    @Autowired
    private IAdvertisementReportService advertisementReportService;

    @Scheduled(fixedDelay = 30000)
    public void updateTodayReport(){
        advertisementReportService.updateTodayReport();
    }

    @Scheduled(cron = "0 30 0 * * ?")
    public void updateYesterdayReport(){
        advertisementReportService.updateYesterdayReport();
    }

}
