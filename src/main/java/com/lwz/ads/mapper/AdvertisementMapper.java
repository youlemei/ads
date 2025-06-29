package com.lwz.ads.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lwz.ads.mapper.entity.Advertisement;
import org.apache.ibatis.annotations.CacheNamespace;

/**
 * <p>
 * 广告 Mapper 接口
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
@CacheNamespace(flushInterval = 60000)
public interface AdvertisementMapper extends BaseMapper<Advertisement> {

}
