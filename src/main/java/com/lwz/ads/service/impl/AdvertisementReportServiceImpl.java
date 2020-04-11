package com.lwz.ads.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwz.ads.bean.CountSum;
import com.lwz.ads.constant.Const;
import com.lwz.ads.mapper.AdvertisementReportMapper;
import com.lwz.ads.mapper.entity.AdvertisementReport;
import com.lwz.ads.mapper.entity.PromoteRecord;
import com.lwz.ads.service.IAdvertisementReportService;
import com.lwz.ads.util.Convert;
import com.lwz.ads.util.DateUtils;
import com.lwz.ads.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * 广告报表, 按日汇总 服务实现类
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
@Slf4j
@Service
public class AdvertisementReportServiceImpl extends ServiceImpl<AdvertisementReportMapper, AdvertisementReport> implements IAdvertisementReportService {

    @Autowired
    private ClickRecordServiceImpl clickRecordService;

    @Autowired
    private ConvertRecordServiceImpl convertRecordService;

    @Autowired
    private PromoteRecordServiceImpl promoteRecordService;

    @Autowired
    private RedisUtils redisUtils;

    @Transactional
    @Override
    public void updateTodayReport() {
        Map<Long, AdvertisementReport> updateMap = new HashMap<>();

        calculateTodayReport(updateMap);

        if (updateMap.size() > 0) {
            updateBatchById(updateMap.values());
            log.info("updateTodayReport updateSize:{}", updateMap.size());
        }
    }

    private void calculateTodayReport(Map<Long, AdvertisementReport> updateMap) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate nowDate = now.toLocalDate();
        String today = now.format(DateUtils.yyyyMMdd);

        redisUtils.execute(redis -> {
            redis.opsForHash().entries(String.format(Const.CLICK_DAY_AMOUNT, today)).forEach((pid, clickSum) -> {
                String[] arr = pid.toString().split("_");
                long adId = Convert.toLong(arr[0]);
                long channelId = Convert.toLong(arr[1]);
                Long reportId = getReportId(nowDate, adId, channelId);
                updateMap.compute(reportId, (id, report) -> Optional.ofNullable(report)
                        .orElseGet(() -> new AdvertisementReport().setId(reportId).setUpdateTime(now))
                        .setClickSum(Convert.toInt(clickSum)));
            });

            redis.opsForHash().entries(String.format(Const.CONVERT_DAY_AMOUNT, today)).forEach((pid, convertSum) -> {
                String[] arr = pid.toString().split("_");
                long adId = Convert.toLong(arr[0]);
                long channelId = Convert.toLong(arr[1]);
                Long reportId = getReportId(nowDate, adId, channelId);
                updateMap.compute(reportId, (id, report) -> Optional.ofNullable(report)
                        .orElseGet(() -> new AdvertisementReport().setId(reportId).setUpdateTime(now))
                        .setSrcConvertSum(Convert.toInt(convertSum)));
            });

            redis.opsForHash().entries(String.format(Const.CONVERT_DAY_ACTUAL_AMOUNT, today)).forEach((pid, actualConvertSum) -> {
                String[] arr = pid.toString().split("_");
                long adId = Convert.toLong(arr[0]);
                long channelId = Convert.toLong(arr[1]);
                Long reportId = getReportId(nowDate, adId, channelId);
                updateMap.compute(reportId, (id, report) -> Optional.ofNullable(report)
                        .orElseGet(() -> new AdvertisementReport().setId(reportId).setUpdateTime(now))
                        .setConvertSum(Convert.toInt(actualConvertSum)));
            });

            redis.keys(String.format(Const.CLICK_DAY_ACTUAL_AMOUNT, today, "*")).forEach(key -> {
                long pid = Convert.toLong(key.substring(key.lastIndexOf(":") + 1));
                PromoteRecord promoteRecord = promoteRecordService.getById(pid);
                Long reportId = getReportId(nowDate, promoteRecord.getAdId(), promoteRecord.getChannelId());
                updateMap.compute(reportId, (id, report) -> Optional.ofNullable(report)
                        .orElseGet(() -> new AdvertisementReport().setId(reportId).setUpdateTime(now))
                        .setDeduplicateClickSum(Convert.toInt(redis.opsForSet().size(key))));
            });
        });
    }

    @Transactional
    @Override
    public void updateYesterdayReport() {

        Map<Long, AdvertisementReport> updateMap = new HashMap<>();

        calculateYesterdayReport(updateMap);

        if (updateMap.size() > 0) {
            updateBatchById(updateMap.values());
            log.info("updateYesterdayReport updateSize:{}", updateMap.size());
        }
    }

    private void calculateYesterdayReport(Map<Long, AdvertisementReport> updateMap) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate yesterday = now.toLocalDate().plusDays(-1);

        List<CountSum> clickSumList = clickRecordService.getBaseMapper().countClickSum(yesterday.format(DateUtils.yyyyMMdd));
        clickSumList.forEach(countSum -> {
            Long reportId = getReportId(yesterday, countSum.getAdId(), countSum.getChannelId());
            updateMap.compute(reportId, (id, report) -> Optional.ofNullable(report)
                    .orElseGet(() -> new AdvertisementReport().setId(reportId).setUpdateTime(now))
                    .setClickSum(countSum.getCount()));
        });

        List<CountSum> deClickSumList = clickRecordService.getBaseMapper().countDeduplicateClickSum(yesterday.format(DateUtils.yyyyMMdd));
        deClickSumList.forEach(countSum -> {
            Long reportId = getReportId(yesterday, countSum.getAdId(), countSum.getChannelId());
            updateMap.compute(reportId, (id, report) -> Optional.ofNullable(report)
                    .orElseGet(() -> new AdvertisementReport().setId(reportId).setUpdateTime(now))
                    .setDeduplicateClickSum(countSum.getCount()));
        });

        List<CountSum> srcConvertSumList = convertRecordService.getBaseMapper().countSrcConvertSum(yesterday.atStartOfDay(), yesterday.atStartOfDay().plusDays(1));
        srcConvertSumList.forEach(countSum -> {
            Long reportId = getReportId(yesterday, countSum.getAdId(), countSum.getChannelId());
            updateMap.compute(reportId, (id, report) -> Optional.ofNullable(report)
                    .orElseGet(() -> new AdvertisementReport().setId(reportId).setUpdateTime(now))
                    .setSrcConvertSum(countSum.getCount()));
        });

        List<CountSum> convertSumList = convertRecordService.getBaseMapper().countConvertSum(yesterday.atStartOfDay(), yesterday.atStartOfDay().plusDays(1));
        convertSumList.forEach(countSum -> {
            Long reportId = getReportId(yesterday, countSum.getAdId(), countSum.getChannelId());
            updateMap.compute(reportId, (id, report) -> Optional.ofNullable(report)
                    .orElseGet(() -> new AdvertisementReport().setId(reportId).setUpdateTime(now))
                    .setConvertSum(countSum.getCount()));
        });
    }


    private Long getReportId(LocalDate adDate, Long adId, Long channelId) {
        AdvertisementReport report = lambdaQuery()
                .eq(AdvertisementReport::getAdId, adId)
                .eq(AdvertisementReport::getChannelId, channelId)
                .eq(AdvertisementReport::getAdDate, adDate).one();
        if (report != null) {
            return report.getId();
        }
        PromoteRecord promoteRecord = promoteRecordService.lambdaQuery()
                .eq(PromoteRecord::getAdId, adId)
                .eq(PromoteRecord::getChannelId, channelId).one();
        AdvertisementReport save = new AdvertisementReport()
                .setAdDate(adDate)
                .setAdId(adId)
                .setChannelId(channelId)
                .setAdCreator(promoteRecord.getAdCreator())
                .setChannelCreator(promoteRecord.getChannelCreator())
                .setInPrice(promoteRecord.getInPrice())
                .setOutPrice(promoteRecord.getOutPrice())
                .setUpdateTime(LocalDateTime.now());
        save(save);
        return save.getId();
    }
}
