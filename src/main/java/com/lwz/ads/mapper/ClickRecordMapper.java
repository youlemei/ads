package com.lwz.ads.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lwz.ads.entity.ClickRecord;
import com.lwz.ads.mapper.bean.CountSum;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * 点击记录, 按日分表 Mapper 接口
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
public interface ClickRecordMapper extends BaseMapper<ClickRecord> {

    void insertWithDate(@Param("me") ClickRecord clickRecord, @Param("date") String date);

    ClickRecord selectByIdWithDate(@Param("clickId") String clickId, @Param("date") String date);

    void updateByIdWithDate(@Param("to") ClickRecord to, @Param("date") String date);

    void updateWithDate(@Param("me") ClickRecord me, @Param("to") ClickRecord to, @Param("date") String date);

    @Update("create table if not exists click_record_${date} like click_record")
    void createTable(String date);

    List<CountSum> countClickSum(String date);

    List<CountSum> countDeduplicateClickSum(String date);
}
