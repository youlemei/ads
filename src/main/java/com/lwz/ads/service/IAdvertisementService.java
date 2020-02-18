package com.lwz.ads.service;

import com.lwz.ads.mapper.entity.Advertisement;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 广告 服务类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
public interface IAdvertisementService extends IService<Advertisement> {

    /**
     * 获取json_data中的指定field
     *
     * @param ad
     * @param field
     * @return
     */
    String getJsonField(Advertisement ad, String field);

}
