package com.lwz.ads.timer;

import com.lwz.ads.service.IClickRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CreateClickTableTimer implements ApplicationRunner {

    @Autowired
    private IClickRecordService clickRecordService;

    /**
     * 建表删表
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void work(){
        clickRecordService.createTable();
        clickRecordService.deleteClickTable();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        clickRecordService.createTable();
    }

}
