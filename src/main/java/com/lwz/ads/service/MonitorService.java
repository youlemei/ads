package com.lwz.ads.service;

import com.alibaba.fastjson.JSONObject;
import com.lwz.ads.service.impl.ClickRecordServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author liweizhou 2021/3/4
 */
@Slf4j
@Service
public class MonitorService {

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private ClickRecordServiceImpl clickRecordService;

    @Scheduled(cron = "30 * * * * ?")
    public void monitor() {

        monitorExecutor(taskExecutor);

        clickRecordService.getExecutorConcurrentMap().values().forEach(this::monitorExecutor);

    }

    private void monitorExecutor(ThreadPoolTaskExecutor taskExecutor) {
        log.info("monitor [{}]{}", taskExecutor.getThreadNamePrefix(), taskExecutor.getThreadPoolExecutor());

        if (taskExecutor.getActiveCount() >= taskExecutor.getCorePoolSize()) {
            log.error("monitor Executor is Full!!! [{}]{}", taskExecutor.getThreadNamePrefix(), taskExecutor.getThreadPoolExecutor());
        }
    }

    public JSONObject monitorStat() {
        JSONObject data = new JSONObject();
        executorStat(data, taskExecutor);
        executorStat(data, clickRecordService.getRetryExecutor());
        clickRecordService.getExecutorConcurrentMap().values().forEach(e -> executorStat(data, e));
        return data;
    }

    private void executorStat(JSONObject data, ThreadPoolTaskExecutor taskExecutor) {
        data.put(taskExecutor.getThreadNamePrefix(), taskExecutor.getThreadPoolExecutor().toString());
    }

}
