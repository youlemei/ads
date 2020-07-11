package com.lwz.ads.timer;

import com.lwz.ads.service.impl.ClickRecordServiceImpl;
import com.lwz.ads.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author liweizhou 2020/2/2
 */
@Component
public class RetryClickHandleTimer {

    @Autowired
    private ClickRecordServiceImpl clickRecordService;

    @Value("${server.port}")
    private int port;

    @Scheduled(fixedDelay = 60000)
    public void work(){
        if (port != 9999) {
            return;
        }

        //重试1分钟前-两天内
        LocalDateTime now = LocalDateTime.now().plusMinutes(-1);

        String yesterday = now.plusDays(-1).format(DateUtils.yyyyMMdd);
        clickRecordService.retryClick(yesterday, null);

        String today = now.format(DateUtils.yyyyMMdd);
        clickRecordService.retryClick(today, now);

    }



}
