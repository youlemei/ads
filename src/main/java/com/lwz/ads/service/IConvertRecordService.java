package com.lwz.ads.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lwz.ads.entity.ClickRecord;
import com.lwz.ads.entity.ConvertRecord;
import com.lwz.ads.entity.PromoteRecord;

import java.time.LocalDateTime;

/**
 * <p>
 * 转化记录 服务类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
public interface IConvertRecordService extends IService<ConvertRecord> {

    /**
     * 处理转化回调
     *
     * @param clickRecord
     * @param promoteRecord
     */
    boolean saveConvert(ClickRecord clickRecord, PromoteRecord promoteRecord);

    /**
     * 异步处理转化
     *
     * @param clickId
     * @param clickTime
     * @param promoteRecord
     */
    void asyncHandleConvert(String clickId, LocalDateTime clickTime, PromoteRecord promoteRecord);

    /**
     * 异步通知渠道转化
     *
     * @param clickId
     */
    void asyncNotifyConvert(String clickId);
}
