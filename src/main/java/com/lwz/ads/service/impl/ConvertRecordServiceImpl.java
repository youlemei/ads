package com.lwz.ads.service.impl;

import com.lwz.ads.entity.ConvertRecord;
import com.lwz.ads.mapper.ConvertRecordMapper;
import com.lwz.ads.service.IConvertRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 转化记录 服务实现类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
@Service
public class ConvertRecordServiceImpl extends ServiceImpl<ConvertRecordMapper, ConvertRecord> implements IConvertRecordService {

    @Override
    public void handleConvert(String date, String clickId) {
        //存表
        //异步, 核减, 回调渠道

    }

}
