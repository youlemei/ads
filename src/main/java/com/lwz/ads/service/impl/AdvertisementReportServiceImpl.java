package com.lwz.ads.service.impl;

import com.lwz.ads.entity.AdvertisementReport;
import com.lwz.ads.mapper.AdvertisementReportMapper;
import com.lwz.ads.service.IAdvertisementReportService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 广告报表, 按日汇总 服务实现类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
@Service
public class AdvertisementReportServiceImpl extends ServiceImpl<AdvertisementReportMapper, AdvertisementReport> implements IAdvertisementReportService {

}
