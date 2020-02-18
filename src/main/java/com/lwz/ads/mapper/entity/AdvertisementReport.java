package com.lwz.ads.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 广告报表, 按日汇总
 * </p>
 *
 * @author lwz
 * @since 2020-02-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class AdvertisementReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 日期
     */
    private LocalDate adDate;

    /**
     * 渠道id
     */
    private Long channelId;

    /**
     * 广告id
     */
    private Long adId;

    /**
     * 渠道创建者
     */
    private String channelCreator;

    /**
     * 广告创建者
     */
    private String adCreator;

    /**
     * 点击
     */
    private Integer clickSum;

    /**
     * 去重点击
     */
    private Integer deduplicateClickSum;

    /**
     * 原始转化
     */
    private Integer srcConvertSum;

    /**
     * 转化
     */
    private Integer convertSum;

    /**
     * 接入单价
     */
    private BigDecimal inPrice;

    /**
     * 投放单价
     */
    private BigDecimal outPrice;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 状态
     */
    private Boolean status;

    private String jsonData;


}
