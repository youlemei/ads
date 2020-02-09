package com.lwz.ads.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwz.ads.constant.Const;
import com.lwz.ads.constant.PromoteStatusEnum;
import com.lwz.ads.constant.TraceTypeEnum;
import com.lwz.ads.entity.Advertisement;
import com.lwz.ads.entity.PromoteRecord;
import com.lwz.ads.mapper.PromoteRecordMapper;
import com.lwz.ads.service.IAdvertisementService;
import com.lwz.ads.service.IPromoteRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

/**
 * <p>
 * 广告投放 服务实现类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
@Slf4j
@Service
public class PromoteRecordServiceImpl extends ServiceImpl<PromoteRecordMapper, PromoteRecord> implements IPromoteRecordService {

    @Autowired
    private IAdvertisementService advertisementService;

    @Value("${system.web.scheme:http}")
    private String scheme;

    @Value("${system.web.domain:localhost:9999}")
    private String domain;

    @Transactional
    @Override
    public void doCreateClickUrl(PromoteRecord promoteRecord) {

        UriComponentsBuilder clickUriBuilder = UriComponentsBuilder.newInstance()
                .scheme(scheme)
                .host(domain)
                .path("click")
                .queryParam("adId", promoteRecord.getAdId())
                .queryParam("channelId", promoteRecord.getChannelId());

        Advertisement ad = advertisementService.getById(promoteRecord.getAdId());
        TraceTypeEnum adTraceType = TraceTypeEnum.valueOfType(ad.getTraceType());
        TraceTypeEnum promoteTraceType = TraceTypeEnum.valueOfType(promoteRecord.getTraceType());
        switch (adTraceType) {
            case REDIRECT:
                clickUriBuilder.queryParam("type", adTraceType.getType());
                break;
            case ASYNC:
                clickUriBuilder.queryParam("type", promoteTraceType.getType());
                break;
            default:break;
        }

        UriComponents adUri = UriComponentsBuilder.fromHttpUrl(ad.getTraceUrl()).build();
        String callback = advertisementService.getJsonField(ad, Const.CALLBACK);
        adUri.getQueryParams().forEach((key, list) -> {
            if (!CollectionUtils.isEmpty(list)) {
                String value = list.get(0);
                if (StringUtils.hasLength(value) && Const.PARAM_PATTERN.matcher(value).matches() && !value.toLowerCase().contains(callback)) {
                    clickUriBuilder.queryParam(key, value);
                }
            }
        });

        clickUriBuilder.queryParam(Const.CALLBACK, "{callback}");
        String clickUri = clickUriBuilder.build().toUriString();
        PromoteRecord to = new PromoteRecord();
        to.setTraceUrl(clickUri);
        to.setPromoteStatus(PromoteStatusEnum.RUNNING.getStatus());
        to.setEditor("system");
        to.setEditTime(LocalDateTime.now());

        update().eq("id", promoteRecord.getId()).eq("promote_status", PromoteStatusEnum.CREATING.getStatus()).update(to);

        log.info("doCreateClickUrl success. adId:{} channelId:{} clickUri:{}", promoteRecord.getAdId(), promoteRecord.getChannelId(), clickUri);
    }

}
