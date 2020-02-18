package com.lwz.ads.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwz.ads.mapper.entity.Advertisement;
import com.lwz.ads.mapper.AdvertisementMapper;
import com.lwz.ads.service.IAdvertisementService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * <p>
 * 广告 服务实现类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
@Service
public class AdvertisementServiceImpl extends ServiceImpl<AdvertisementMapper, Advertisement> implements IAdvertisementService {

    @Override
    public String getJsonField(Advertisement ad, String field) {
        if (StringUtils.hasLength(ad.getJsonData())) {
            JSONObject jsonData = JSON.parseObject(ad.getJsonData());
            String value = jsonData.getString(field);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return field;
    }

}
