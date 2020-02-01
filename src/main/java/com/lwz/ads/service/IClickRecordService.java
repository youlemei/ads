package com.lwz.ads.service;

import com.lwz.ads.entity.ClickRecord;
import com.baomidou.mybatisplus.extension.service.IService;

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
     * @param adId 广告id
     * @param channelId 渠道id
     * @param request 参数
     */
    void handleClick(Long adId, Long channelId, Map<String, Object> request);

}
