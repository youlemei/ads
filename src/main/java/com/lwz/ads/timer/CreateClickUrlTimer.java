package com.lwz.ads.timer;

import com.lwz.ads.constant.PromoteStatusEnum;
import com.lwz.ads.mapper.entity.PromoteRecord;
import com.lwz.ads.service.impl.PromoteRecordServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CreateClickUrlTimer {

    @Autowired
    private PromoteRecordServiceImpl promoteRecordService;

    @Scheduled(fixedDelay = 3000)
    public void work() {
        promoteRecordService.lambdaQuery().eq(PromoteRecord::getPromoteStatus, PromoteStatusEnum.CREATING.getStatus()).list()
                .parallelStream()
                .forEach(promoteRecord -> {
                    try {
                        promoteRecordService.doCreateClickUrl(promoteRecord);
                    } catch (Exception e) {
                        log.error("doCreateClickUrl error. id:{} msg:{}", promoteRecord.getId(), e.getMessage(), e);
                    }
                });
    }

}
