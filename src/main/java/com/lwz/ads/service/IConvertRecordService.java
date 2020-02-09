package com.lwz.ads.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lwz.ads.entity.ClickRecord;
import com.lwz.ads.entity.ConvertRecord;
import com.lwz.ads.entity.PromoteRecord;

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
     * @param date
     * @return
     */
    ConvertRecord saveConvert(ClickRecord clickRecord, PromoteRecord promoteRecord, String date);

    /**
     * 异步通知渠道转化
     *
     * @param convertRecord
     */
    void asyncNotifyConvert(ConvertRecord convertRecord);
}
