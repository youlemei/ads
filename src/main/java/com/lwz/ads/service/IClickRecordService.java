package com.lwz.ads.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lwz.ads.entity.Advertisement;
import com.lwz.ads.entity.Channel;
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
     * @param channel
     * @return 点击id
     */
    String saveClick(LocalDateTime clickTime, Map<String, Object> request, String type, PromoteRecord promoteRecord, Advertisement ad, Channel channel);

    /**
     * 异步通知广告点击
     *
     * @param clickId 点击id
     * @param clickTime
     * @param ad
     */
    void asyncHandleClick(String clickId, LocalDateTime clickTime, Advertisement ad);

    /**
     * 通知广告点击, 响应302跳转
     *
     * @param clickId
     * @param clickTime
     * @param ad
     * @return
     */
    URI redirectHandleClick(String clickId, LocalDateTime clickTime, Advertisement ad);
}
