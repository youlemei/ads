package com.lwz.ads.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lwz.ads.bean.PageResponse;
import com.lwz.ads.mapper.entity.ClickRecord;
import com.lwz.ads.service.impl.ClickRecordServiceImpl;
import com.lwz.ads.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * @author liweizhou 2020/6/8
 */
@Slf4j
@RestController
@RequestMapping("/clickRecord")
public class ClickRecordController {

    @Autowired
    private ClickRecordServiceImpl clickRecordService;

    @GetMapping
    public PageResponse<ClickRecord> list(Long adId, Long channelId, String mac, String date,
                                         @RequestParam(defaultValue = "1") long pageIndex,
                                         @RequestParam(defaultValue = "10") long pageSize) {
        log.info("list adId:{} channelId:{} mac:{} date:{}", adId, channelId, mac, date);
        if (StringUtils.isEmpty(date)) {
            date = DateUtils.yyyyMMdd.format(LocalDate.now());
        } else {
            date = date.replaceAll("[^0-9]", "");
        }
        Page<ClickRecord> page = new Page<>(pageIndex, pageSize);
        IPage<ClickRecord> pageInfo = clickRecordService.getBaseMapper().selectPageWithDate(page, adId, channelId, mac, date);
        return PageResponse.success(pageInfo.getCurrent(), pageInfo.getSize(), pageInfo.getTotal(), pageInfo.getRecords());
    }

}
