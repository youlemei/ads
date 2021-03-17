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

    @Scheduled(fixedDelay = 10000)
    public void work(){
        if (port != 9999) {
            return;
        }

        String yesterday = LocalDateTime.now().plusDays(-1).format(DateUtils.yyyyMMdd);
        clickRecordService.retryClick(yesterday);

        String today = LocalDateTime.now().format(DateUtils.yyyyMMdd);
        clickRecordService.retryClick(today);

    }

}
