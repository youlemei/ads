package com.lwz.ads.service;

import com.lwz.ads.entity.PromoteRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 广告投放 服务类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
public interface IPromoteRecordService extends IService<PromoteRecord> {

    /**
     * 生成推广点击链接
     *
     * @param promoteRecord 推广信息
     */
    void doCreateClickUrl(PromoteRecord promoteRecord);

}
