package com.lwz.ads.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author liweizhou 2021/3/4
 */
@Slf4j
@Service
public class MonitorService {

    @Autowired
    private ThreadPoolTaskExecutor applicationTaskExecutor;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private ThreadPoolExecutor retryExecutor;

    @Scheduled(cron = "30 * * * * ?")
    public void monitor() {

        log.info("monitor taskExecutor:{}", applicationTaskExecutor.getThreadPoolExecutor());

        log.info("monitor taskScheduler:{}", taskScheduler.getScheduledExecutor());

        log.info("monitor retryExecutor:{}", retryExecutor);

    }
}
