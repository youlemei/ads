package com.lwz.ads.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lwz.ads.bean.PageResponse;
import com.lwz.ads.constant.Const;
import com.lwz.ads.mapper.entity.ClickRecord;
import com.lwz.ads.service.impl.ClickRecordServiceImpl;
import com.lwz.ads.util.DateUtils;
import com.lwz.ads.util.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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
    public PageResponse<JSONObject> list(Long adId, Long channelId, String mac, String date,
                                   HttpServletRequest request,
                                   @RequestParam(defaultValue = "1") long pageIndex,
                                   @RequestParam(defaultValue = "10") long pageSize) {
        String ip = IPUtils.getRealIp(request);
        log.info("list adId:{} channelId:{} mac:{} date:{} ip:{}", adId, channelId, mac, date, ip);

        if (!IPUtils.isLocalhost(ip)) {
            return PageResponse.empty();
        }

        if (StringUtils.isEmpty(date)) {
            date = DateUtils.yyyyMMdd.format(LocalDate.now());
        } else {
            date = date.replaceAll("[^0-9]", "");
        }
        Page<ClickRecord> page = new Page<>(pageIndex, pageSize);
        IPage<ClickRecord> pageInfo = clickRecordService.getBaseMapper().selectPageWithDate(page, adId, channelId, mac, date);
        List<JSONObject> list = pageInfo.getRecords().stream().map(clickRecord -> {
            JSONObject item = new JSONObject();
            item.put("id", clickRecord.getId());
            item.put("adId", clickRecord.getAdId());
            item.put("channelId", clickRecord.getChannelId());
            item.put("createTime", clickRecord.getCreateTime());
            item.put("mac", clickRecord.getMac());
            item.put("ip", clickRecord.getIp());
            item.put("callback", JSON.parseObject(clickRecord.getParamJson()).get(Const.CALLBACK));
            item.put("clickStatus", clickRecord.getClickStatus());
            return item;
        }).collect(Collectors.toList());
        return PageResponse.success(pageInfo.getCurrent(), pageInfo.getSize(), pageInfo.getTotal(), list);
    }

}
