package com.lwz.ads.service.impl;

import com.lwz.ads.constant.PromoteStatusEnum;
import com.lwz.ads.entity.Advertisement;
import com.lwz.ads.entity.Channel;
import com.lwz.ads.entity.ClickRecord;
import com.lwz.ads.entity.PromoteRecord;
import com.lwz.ads.mapper.ClickRecordMapper;
import com.lwz.ads.service.IAdvertisementService;
import com.lwz.ads.service.IChannelService;
import com.lwz.ads.service.IClickRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwz.ads.service.IPromoteRecordService;
import com.lwz.ads.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>
 * 点击记录, 按日分表 服务实现类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
@Service
public class ClickRecordServiceImpl extends ServiceImpl<ClickRecordMapper, ClickRecord> implements IClickRecordService {

    @Autowired
    private IPromoteRecordService promoteRecordService;

    @Autowired
    private IAdvertisementService advertisementService;

    @Autowired
    private IChannelService channelService;

    @Override
    public void handleClick(Long adId, Long channelId, Map<String, Object> request) {

        //缓存
        PromoteRecord promoteRecord = promoteRecordService.getOne(promoteRecordService.lambdaQuery().eq(PromoteRecord::getAdId, adId).eq(PromoteRecord::getChannelId, channelId));
        if (promoteRecord == null) {

        }
        Advertisement ad = advertisementService.getById(adId);
        if (ad == null) {

        }
        Channel channel = channelService.getById(channelId);
        if (channel == null) {

        }

        LocalDateTime now = LocalDateTime.now();
        PromoteStatusEnum promoteStatus = PromoteStatusEnum.valueOfStatus(promoteRecord.getPromoteStatus());
        if (!ad.getTraceStatus() || now.isAfter(ad.getEndTime()) || promoteStatus != PromoteStatusEnum.RUNNING) {

        }

        //存表, 分表
        String ip = null;
        String mac = null;
        UriComponents adUri = UriComponentsBuilder.fromHttpUrl(ad.getTraceUrl()).build();
        for (Map.Entry<String, List<String>> entry : adUri.getQueryParams().entrySet()) {
            String key = entry.getKey();
            List<String> list = entry.getValue();
            if (!CollectionUtils.isEmpty(list)) {
                String value = list.get(0);
                if (StringUtils.hasLength(value) && PromoteRecordServiceImpl.PARAM_PATTERN.matcher(value).matches()) {
                    String lowerCaseKey = key.toLowerCase();
                    if (lowerCaseKey.contains("ip")) {

                    }
                    if (lowerCaseKey.contains("idfa")) {

                    }
                }
            }
        }
        String date = now.format(DateUtils.yyyyMMdd);
        ClickRecord clickRecord = new ClickRecord()
                .setAdId(adId)
                .setAdCreator(ad.getCreator())
                .setChannelId(channelId)
                .setChannelCreator(channel.getCreator())
                .setCreateTime(now)
                .setIp()
                .setMac()
                .setParamJson();

        //异步调用广告主

    }

}
