package com.lwz.ads.timer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.lwz.ads.entity.PromoteRecord;
import com.lwz.ads.mapper.PromoteRecordMapper;
import com.lwz.ads.service.IPromoteRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sound.sampled.Line;
import java.util.List;

@Slf4j
@Component
public class CreateClickUrlTimer {

    @Autowired
    private IPromoteRecordService promoteRecordService;

    @Scheduled(fixedDelay = 5000)
    public void work(){
        //暂时单机版
        List<PromoteRecord> promoteRecordList = promoteRecordService.list(promoteRecordService.lambdaQuery().eq(PromoteRecord::getPromoteStatus, 0));
        promoteRecordList.parallelStream().forEach(promoteRecord -> {
            try {
                promoteRecordService.doCreateClickUrl(promoteRecord);
            } catch (Exception e) {
                log.error("work error. msg:{}", e.getMessage(), e);
            }
        });
        log.info("CreateClickUrlTimer done {}.", promoteRecordList.size());
    }

}
