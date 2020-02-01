package com.lwz.ads.service.impl;

import com.lwz.ads.constant.PromoteStatusEnum;
import com.lwz.ads.constant.TraceTypeEnum;
import com.lwz.ads.entity.Advertisement;
import com.lwz.ads.entity.PromoteRecord;
import com.lwz.ads.mapper.PromoteRecordMapper;
import com.lwz.ads.service.IAdvertisementService;
import com.lwz.ads.service.IPromoteRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * <p>
 * 广告投放 服务实现类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
@Service
public class PromoteRecordServiceImpl extends ServiceImpl<PromoteRecordMapper, PromoteRecord> implements IPromoteRecordService {

    public static final Pattern PARAM_PATTERN = Pattern.compile("^\\{[a-zA-Z0-9]+}$");

    @Autowired
    private IAdvertisementService advertisementService;

    @Value("${system.web.scheme:http}")
    private String scheme;

    @Value("${system.web.domain:localhost}")
    private String domain;

    @Transactional
    @Override
    public void doCreateClickUrl(PromoteRecord promoteRecord) {

        UriComponentsBuilder clickUriBuilder = UriComponentsBuilder.newInstance()
                .scheme(scheme)
                .host(domain)
                .queryParam("adId", promoteRecord.getAdId())
                .queryParam("channelId", promoteRecord.getChannelId());

        Advertisement ad = advertisementService.getById(promoteRecord.getAdId());
        TraceTypeEnum adTraceType = TraceTypeEnum.valueOfCode(ad.getTraceType());
        TraceTypeEnum promoteTraceType = TraceTypeEnum.valueOfCode(promoteRecord.getTraceType());
        switch (adTraceType) {
            case REDIRECT:
                clickUriBuilder.queryParam("type", adTraceType.getCode());
                break;
            case ASYNC:
                clickUriBuilder.queryParam("type", promoteTraceType.getCode());
                break;
            default:break;
        }

        UriComponents adUri = UriComponentsBuilder.fromHttpUrl(ad.getTraceUrl()).build();
        adUri.getQueryParams().forEach((key, list) -> {
            if (!CollectionUtils.isEmpty(list)) {
                String value = list.get(0);
                if (StringUtils.hasLength(value) && PARAM_PATTERN.matcher(value).matches()) {
                    clickUriBuilder.queryParam(key, value);
                }
            }
        });

        String clickUri = clickUriBuilder.build().toUriString();
        PromoteRecord to = new PromoteRecord();
        to.setTraceUrl(clickUri);
        to.setPromoteStatus(PromoteStatusEnum.RUNNING.getStatus());
        to.setEditor("doCreateClickUrl");
        to.setEditTime(LocalDateTime.now());

        update(to, update().eq("id", promoteRecord.getId()).eq("promote_status", PromoteStatusEnum.CREATING.getStatus()));

    }

}
