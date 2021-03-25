package com.lwz.ads.timer;

import com.lwz.ads.service.impl.ConvertRecordServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author liweizhou 2020/2/2
 */
@Component
public class RetryConvertHandleTimer {

    @Autowired
    private ConvertRecordServiceImpl convertRecordService;

    @Scheduled(fixedDelay = 60000)
    public void retryConvert(){

        //重试1分钟前-两天内
        LocalDateTime now = LocalDateTime.now().plusMinutes(-1);
        LocalDateTime start = now.plusDays(-2);
        LocalDateTime end = now;

        convertRecordService.retryConvert(start, end);
    }

}
