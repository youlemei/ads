package com.lwz.ads.service;

import com.lwz.ads.mapper.entity.AdvertisementReport;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;

/**
 * <p>
 * 广告报表, 按日汇总 服务类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
public interface IAdvertisementReportService extends IService<AdvertisementReport> {

    /**
     * 更新今日点击/转化
     */
    void updateReportWithRedis();

    /**
     * 统计昨日点击/转化
     */
    void updateReportWithMySQL(LocalDate date);
}
