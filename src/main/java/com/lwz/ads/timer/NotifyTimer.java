package com.lwz.ads.timer;

import com.lwz.ads.bean.WeChatRobotMsg;
import com.lwz.ads.constant.Const;
import com.lwz.ads.mapper.entity.ConvertRecord;
import com.lwz.ads.service.WeChatRobotService;
import com.lwz.ads.service.impl.ClickRecordServiceImpl;
import com.lwz.ads.service.impl.ConvertRecordServiceImpl;
import com.lwz.ads.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author liweizhou 2020/2/28
 */
@Component
public class NotifyTimer {

    @Autowired
    private ClickRecordServiceImpl clickRecordService;

    @Autowired
    private ConvertRecordServiceImpl convertRecordService;

    @Autowired
    private WeChatRobotService weChatRobotService;

    @Scheduled(cron = "0 0 10 * * ?")
    public void work(){
        LocalDate nowDate = LocalDate.now();

        String yesterday = nowDate.plusDays(-1).format(DateUtils.yyyyMMdd);
        long clickCount = clickRecordService.getBaseMapper().countRetryMax(yesterday);

        LocalDateTime start = nowDate.plusDays(-1).atStartOfDay();
        LocalDateTime end = nowDate.atStartOfDay();
        Integer convertCount = convertRecordService.lambdaQuery()
                .between(ConvertRecord::getCreateTime, start, end)
                .ge(ConvertRecord::getRetryTimes, 3)
                .count();

        if (clickCount > 0 || convertCount > 0) {
            WeChatRobotMsg msg = WeChatRobotMsg.buildText()
                    .content(String.format("重试告警: 点击重试超过3次的记录数:%s 转化重试超过3次的记录数:%s", clickCount, convertCount))
                    .build();
            weChatRobotService.notify(Const.WECHAT_ROBOT_URL, msg);
        }

    }

}
