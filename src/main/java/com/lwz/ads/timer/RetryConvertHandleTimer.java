package com.lwz.ads.timer;

import com.lwz.ads.constant.ConvertStatusEnum;
import com.lwz.ads.entity.ConvertRecord;
import com.lwz.ads.service.impl.ConvertRecordServiceImpl;
import com.lwz.ads.util.DateUtils;
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

    @Scheduled(fixedDelay = 20000)
    public void work(){
        LocalDateTime now = LocalDateTime.now().plusMinutes(-1);
        String start = now.plusDays(-2).format(DateUtils.DEFAULT_FORMATTER);
        String end = now.format(DateUtils.DEFAULT_FORMATTER);

        //通知渠道
        convertRecordService.list(convertRecordService.lambdaQuery()
                .between(ConvertRecord::getCreateTime, start, end)
                .eq(ConvertRecord::getConvertStatus, ConvertStatusEnum.CONVERTED.getStatus()))
                .forEach(convertRecord -> convertRecordService.asyncNotifyConvert(convertRecord.getClickId()));

    }

}
