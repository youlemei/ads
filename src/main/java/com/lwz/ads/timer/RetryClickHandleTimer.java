package com.lwz.ads.timer;

import com.lwz.ads.constant.ClickStatusEnum;
import com.lwz.ads.constant.TraceTypeEnum;
import com.lwz.ads.mapper.entity.Advertisement;
import com.lwz.ads.mapper.entity.ClickRecord;
import com.lwz.ads.service.impl.AdvertisementServiceImpl;
import com.lwz.ads.service.impl.ClickRecordServiceImpl;
import com.lwz.ads.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AdvertisementServiceImpl advertisementService;

    @Scheduled(fixedDelay = 20000)
    public void work(){

        //重试1分钟前-两天内
        LocalDateTime now = LocalDateTime.now().plusMinutes(-1);

        String today = now.format(DateUtils.yyyyMMdd);
        clickRecordService.getBaseMapper().selectReceiveClick(now, today)
                .forEach(clickRecord -> doRetry(today, clickRecord));

        String yesterday = now.plusDays(-1).format(DateUtils.yyyyMMdd);
        clickRecordService.getBaseMapper().selectReceiveClick(null, yesterday)
                .forEach(clickRecord -> doRetry(yesterday, clickRecord));
    }

    private void doRetry(String date, ClickRecord clickRecord) {
        Advertisement ad = advertisementService.getById(clickRecord.getAdId());
        TraceTypeEnum adTraceType = TraceTypeEnum.valueOfType(ad.getTraceType());
        if (adTraceType == TraceTypeEnum.REDIRECT) {
            //丢弃
            ClickRecord to = new ClickRecord();
            to.setClickStatus(ClickStatusEnum.DISCARDED.getStatus());
            to.setEditor("system");
            to.setEditTime(LocalDateTime.now());
            clickRecordService.getBaseMapper().updateByIdWithDate(to, date);
        } else {
            clickRecordService.asyncHandleClick(clickRecord, ad);
        }
    }

}
