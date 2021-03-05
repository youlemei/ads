package com.lwz.ads.service;

import com.lwz.ads.util.RedisUtils;
import com.lwz.ads.util.SmartRejectedExecutionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author liweizhou 2021/3/4
 */
@Slf4j
@Service
public class MonitorService {

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private RedisUtils redisUtils;

    private ThreadPoolExecutor retryExecutor = new ThreadPoolExecutor(
            100, 100,
            0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10000),
            new CustomizableThreadFactory("retry-executor-"),
            new SmartRejectedExecutionHandler()
    );

    public ExecutorService getRetryExecutor() {
        return retryExecutor;
    }

    @Scheduled(cron = "30 * * * * ?")
    public void monitor() {

        log.info("monitor taskExecutor:{}", taskExecutor.getThreadPoolExecutor());

        log.info("monitor taskScheduler:{}", taskScheduler.getScheduledExecutor());

        log.info("monitor retryExecutor:{}", retryExecutor);

    }

    @PreDestroy
    public void destroy() {
        retryExecutor.shutdownNow();
    }
}
