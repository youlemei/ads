package com.lwz.ads.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lwz.ads.bean.CountSum;
import com.lwz.ads.mapper.entity.ClickRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    int insertWithDate(@Param("me") ClickRecord clickRecord, @Param("date") String date);

    ClickRecord selectByIdWithDate(@Param("clickId") String clickId, @Param("date") String date);

    int updateByIdWithDate(@Param("to") ClickRecord to, @Param("date") String date);

    int updateWithDate(@Param("me") ClickRecord me, @Param("to") ClickRecord to, @Param("date") String date);

    IPage<ClickRecord> selectPageWithDate(Page<ClickRecord> page,
                                          @Param("adId") Long adId, @Param("channelId") Long channelId,
                                          @Param("mac") String mac, @Param("date") String date);

    /**
     * 统计
     */
    List<CountSum> countClickSum(String date);

    List<CountSum> countDeduplicateClickSum(String date);

    /**
     * 重试
     */
    List<ClickRecord> selectReceiveClick(@Param("date") String date, @Param("limit") int limit);

    @Update("update click_record_${date} set retry_times = retry_times + 1, edit_time = now() where id = #{clickId}")
    int incrRetryTimes(@Param("clickId") String clickId, @Param("date") String date);

    @Select("select count(*) from click_record_${date} where retry_times >= 3")
    long countRetryMax(String date);

    /**
     * 建表
     */
    @Update("create table if not exists click_record_${date} like click_record")
    int createTable(String date);

    @Update("drop table if exists click_record_${date}")
    int deleteTable(String date);
}
