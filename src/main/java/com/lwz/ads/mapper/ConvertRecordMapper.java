package com.lwz.ads.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lwz.ads.mapper.entity.ConvertRecord;
import com.lwz.ads.bean.CountSum;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 转化记录 Mapper 接口
 * </p>
 *
 * @author lwz
 * @since 2020-01-30
 */
public interface ConvertRecordMapper extends BaseMapper<ConvertRecord> {

    List<CountSum> countSrcConvertSum(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<CountSum> countConvertSum(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Update("update convert_record set retry_times = retry_times + 1, edit_time = now() where click_id = #{clickId}")
    int incrRetryTimes(String clickId);
}
