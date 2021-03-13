package com.lwz.ads.service;

import com.lwz.ads.util.SmartRejectedExecutionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executor;

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
    private SysConfigLoader sysConfigLoader;

    @Autowired
    private TaskDecorator taskDecorator;

    private ThreadPoolTaskExecutor retryExecutor;

    @PostConstruct
    public void init() {
        retryExecutor = new ThreadPoolTaskExecutor();
        retryExecutor.setCorePoolSize(100);
        retryExecutor.setMaxPoolSize(100);
        retryExecutor.setTaskDecorator(taskDecorator);
        retryExecutor.setQueueCapacity(5000);
        retryExecutor.setThreadNamePrefix("retry-executor-");
        retryExecutor.setRejectedExecutionHandler(new SmartRejectedExecutionHandler());
        retryExecutor.initialize();
    }

    public Executor getRetryExecutor() {
        return retryExecutor;
    }

    @Scheduled(cron = "30 * * * * ?")
    public void monitor() {

        log.info("monitor taskExecutor:{}", taskExecutor.getThreadPoolExecutor());

        log.info("monitor taskScheduler:{}", taskScheduler.getScheduledExecutor());

        log.info("monitor retryExecutor:{}", retryExecutor);

        log.info("monitor sysConfigLoader:{}", sysConfigLoader.monitor());

    }

    @PreDestroy
    public void destroy() {
        retryExecutor.shutdown();
    }
}
