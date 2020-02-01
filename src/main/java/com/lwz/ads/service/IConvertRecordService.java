package com.lwz.ads.service;

import com.lwz.ads.entity.ConvertRecord;
import com.baomidou.mybatisplus.extension.service.IService;

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
     * @param date 日期
     * @param clickId 点击id
     */
    void handleConvert(String date, String clickId);

}
