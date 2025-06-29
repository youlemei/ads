package com.lwz.ads.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwz.ads.constant.Const;
import com.lwz.ads.constant.PromoteStatusEnum;
import com.lwz.ads.constant.TraceTypeEnum;
import com.lwz.ads.mapper.PromoteRecordMapper;
import com.lwz.ads.mapper.entity.Advertisement;
import com.lwz.ads.mapper.entity.PromoteRecord;
import com.lwz.ads.service.IPromoteRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
    private AdvertisementServiceImpl advertisementService;

    @Value("${system.web.scheme:http}")
    private String scheme;

    @Value("${system.web.domain:localhost:9999}")
    private String domain;

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

        String clickUri = buildClickUrl(clickUriBuilder, ad.getTraceUrl());

        PromoteRecord to = new PromoteRecord();
        to.setInPrice(ad.getInPrice());
        to.setTraceUrl(clickUri);
        to.setPromoteStatus(PromoteStatusEnum.RUNNING.getStatus());
        to.setEditor("system");
        to.setEditTime(LocalDateTime.now());

        update().eq("id", promoteRecord.getId()).eq("promote_status", PromoteStatusEnum.CREATING.getStatus()).update(to);

        log.info("doCreateClickUrl success. adId:{} channelId:{} clickUri:{}", promoteRecord.getAdId(), promoteRecord.getChannelId(), clickUri);
    }

    private String buildClickUrl(UriComponentsBuilder clickUriBuilder, String traceUrl) {
        UriComponents adUri = UriComponentsBuilder.fromHttpUrl(traceUrl).build();
        adUri.getQueryParams().forEach((key, list) -> {
            if (!CollectionUtils.isEmpty(list)) {
                String value = list.get(0);
                if (StringUtils.hasLength(value) && Const.PARAM_PATTERN.matcher(value).matches()) {

                    if (value.toLowerCase().contains(Const.CALLBACK)) {
                        clickUriBuilder.queryParam(Const.CALLBACK, String.format("{%s}", Const.CALLBACK));
                    }
                    else if (value.toLowerCase().contains(Const.IP)) {
                        clickUriBuilder.queryParam(Const.IP, String.format("{%s}", Const.IP));
                    }
                    else if (value.toLowerCase().contains(Const.IDFA)) {
                        clickUriBuilder.queryParam(Const.IDFA, String.format("{%s}", Const.IDFA));
                    }
                    else if (value.toLowerCase().contains(Const.IMEI)) {
                        clickUriBuilder.queryParam(Const.IMEI, String.format("{%s}", Const.IMEI));
                    }
                    else if (value.toLowerCase().contains(Const.DT)
                            || value.toLowerCase().contains(Const.TS)
                            || value.toLowerCase().contains(Const.TMS)
                            || value.toLowerCase().contains(Const.SIGN)
                            || value.toLowerCase().contains(Const.CLICK_ID)
                    ) {
                        //skip
                    }
                    else {
                        clickUriBuilder.queryParam(key, value);
                    }

                }
            }
        });

        return clickUriBuilder.build().toUriString();
    }

    public static void main(String[] args) {
        PromoteRecordServiceImpl service = new PromoteRecordServiceImpl();
        UriComponentsBuilder clickUriBuilder = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host("localhost")
                .path("click")
                .queryParam("adId", 1)
                .queryParam("channelId", 2);
        String traceUrl = "https://alsc-ug-monitor-callback.alibaba.com/streamMonitor/click?request_id={click_id}&idfa={idfa}&os=1&mts={tms}&callback_url={callback}&signature={sign}&ip={ip}";
        System.out.println(service.buildClickUrl(clickUriBuilder, traceUrl));
    }

}
