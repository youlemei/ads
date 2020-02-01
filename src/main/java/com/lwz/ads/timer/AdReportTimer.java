package com.lwz.ads.timer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdReportTimer {

    @Scheduled(fixedDelay = 10000)
    public void work(){
        //统计每日点击/转化报表
    }

}
