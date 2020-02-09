package com.lwz.ads.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lwz.ads.entity.Advertisement;
import com.lwz.ads.entity.ClickRecord;
import com.lwz.ads.entity.PromoteRecord;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 * 点击记录, 按日分表 服务类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
public interface IClickRecordService extends IService<ClickRecord> {

    /**
     * 处理广告点击
     *
     * @param clickTime
     * @param request
     * @param type
     * @param promoteRecord
     * @param ad
     * @return 点击id
     */
    ClickRecord saveClick(LocalDateTime clickTime, Map<String, Object> request, String type, PromoteRecord promoteRecord, Advertisement ad);

    /**
     * 异步通知广告点击
     *
     * @param clickRecord
     * @param ad
     */
    void asyncHandleClick(ClickRecord clickRecord, Advertisement ad);

    /**
     * 通知广告点击, 响应302跳转
     *
     * @param clickRecord
     * @param ad
     * @return
     */
    URI redirectHandleClick(ClickRecord clickRecord, Advertisement ad);

    /**
     * 建未来30天的分表
     */
    void createTable();
}
