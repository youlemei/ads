package com.lwz.ads.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lwz.ads.entity.Company;

/**
 * <p>
 * 广告主 Mapper 接口
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
//@CacheNamespace(implementation = ScheduledCache.class, flushInterval = 60000) //漂亮的包装设计模式
public interface CompanyMapper extends BaseMapper<Company> {

}
