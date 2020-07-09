package com.lwz.ads.timer;

import com.lwz.ads.service.IAdvertisementReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 触发式更新到redis/定时同步到报表 + 次日计算报表
 */
@Component
public class AdReportTimer {

    @Autowired
    private IAdvertisementReportService advertisementReportService;

    @Scheduled(fixedDelay = 30000)
    public void updateTodayReport(){
        advertisementReportService.updateReportWithRedis();
    }

    @Scheduled(cron = "0 30 0 * * ?")
    public void updateYesterdayReport(){
        advertisementReportService.updateReportWithMySQL(LocalDate.now().plusDays(-1));
    }

}
