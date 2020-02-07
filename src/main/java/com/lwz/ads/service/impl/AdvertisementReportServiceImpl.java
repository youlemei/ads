package com.lwz.ads.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwz.ads.entity.AdvertisementReport;
import com.lwz.ads.entity.PromoteRecord;
import com.lwz.ads.mapper.AdvertisementReportMapper;
import com.lwz.ads.mapper.bean.CountSum;
import com.lwz.ads.service.IAdvertisementReportService;
import com.lwz.ads.util.DateUtils;
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

    @Transactional
    @Override
    public void countAdReport() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        Map<Long, AdvertisementReport> updateMap = new HashMap<>();
        if (now.getHour() == 0 && now.getMinute() < 30) {
            LocalDate yesterday = today.plusDays(-1);
            updateClickSum(yesterday, updateMap);
            updateDeduplicateClickSum(yesterday, updateMap);

            updateSrcConvertSum(yesterday, updateMap);
            updateConvertSum(yesterday, updateMap);
        }
        updateClickSum(today, updateMap);
        updateDeduplicateClickSum(today, updateMap);

        updateSrcConvertSum(today, updateMap);
        updateConvertSum(today, updateMap);

        if (updateMap.size() > 0) {
            updateBatchById(updateMap.values());
            log.info("countAdReport updateSize:{}", updateMap.size());
        }
    }

    private void updateClickSum(LocalDate adDate, Map<Long, AdvertisementReport> updateMap) {
        List<CountSum> countSumList = clickRecordService.getBaseMapper().countClickSum(adDate.format(DateUtils.yyyyMMdd));
        countSumList.forEach(countSum -> {
            Long reportId = getReportId(adDate, countSum);
            updateMap.compute(reportId, (id, report) -> Optional.ofNullable(report)
                    .orElseGet(() -> new AdvertisementReport().setId(reportId).setUpdateTime(LocalDateTime.now()))
                    .setClickSum(countSum.getCount()));
        });
    }

    private void updateDeduplicateClickSum(LocalDate adDate, Map<Long, AdvertisementReport> updateMap) {
        List<CountSum> countSumList = clickRecordService.getBaseMapper().countDeduplicateClickSum(adDate.format(DateUtils.yyyyMMdd));
        countSumList.forEach(countSum -> {
            Long reportId = getReportId(adDate, countSum);
            updateMap.compute(reportId, (id, report) -> Optional.ofNullable(report)
                    .orElseGet(() -> new AdvertisementReport().setId(reportId).setUpdateTime(LocalDateTime.now()))
                    .setDeduplicateClickSum(countSum.getCount()));
        });
    }

    private void updateSrcConvertSum(LocalDate adDate, Map<Long, AdvertisementReport> updateMap) {
        List<CountSum> countSumList = convertRecordService.getBaseMapper().countSrcConvertSum(adDate.atStartOfDay(), adDate.atStartOfDay().plusDays(1));
        countSumList.forEach(countSum -> {
            Long reportId = getReportId(adDate, countSum);
            updateMap.compute(reportId, (id, report) -> Optional.ofNullable(report)
                    .orElseGet(() -> new AdvertisementReport().setId(reportId).setUpdateTime(LocalDateTime.now()))
                    .setSrcConvertSum(countSum.getCount()));
        });
    }

    private void updateConvertSum(LocalDate adDate, Map<Long, AdvertisementReport> updateMap) {
        List<CountSum> countSumList = convertRecordService.getBaseMapper().countConvertSum(adDate.atStartOfDay(), adDate.atStartOfDay().plusDays(1));
        countSumList.forEach(countSum -> {
            Long reportId = getReportId(adDate, countSum);
            updateMap.compute(reportId, (id, report) -> Optional.ofNullable(report)
                    .orElseGet(() -> new AdvertisementReport().setId(reportId).setUpdateTime(LocalDateTime.now()))
                    .setConvertSum(countSum.getCount()));
        });
    }

    private Long getReportId(LocalDate adDate, CountSum countSum) {
        AdvertisementReport report = lambdaQuery()
                .eq(AdvertisementReport::getAdDate, adDate)
                .eq(AdvertisementReport::getAdId, countSum.getAdId())
                .eq(AdvertisementReport::getChannelId, countSum.getChannelId()).one();
        if (report != null) {
            return report.getId();
        }
        PromoteRecord promoteRecord = promoteRecordService.lambdaQuery()
                .eq(PromoteRecord::getAdId, countSum.getAdId())
                .eq(PromoteRecord::getChannelId, countSum.getChannelId()).one();
        AdvertisementReport save = new AdvertisementReport()
                .setAdDate(adDate)
                .setAdId(countSum.getAdId())
                .setChannelId(countSum.getChannelId())
                .setAdCreator(promoteRecord.getAdCreator())
                .setChannelCreator(promoteRecord.getChannelCreator())
                .setClickSum(0)
                .setDeduplicateClickSum(0)
                .setSrcConvertSum(0)
                .setConvertSum(0)
                .setInPrice(promoteRecord.getInPrice())
                .setOutPrice(promoteRecord.getOutPrice())
                .setUpdateTime(LocalDateTime.now());
        save(save);
        return save.getId();
    }
}
