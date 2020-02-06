package com.lwz.ads.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 广告投放
 * </p>
 *
 * @author lwz
 * @since 2020-02-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class PromoteRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 广告id
     */
    private Long adId;

    /**
     * 渠道id
     */
    private Long channelId;

    /**
     * 广告创建者
     */
    private String adCreator;

    /**
     * 渠道创建者
     */
    private String channelCreator;

    /**
     * 推广状态 0.生成中 1.运行 2.暂停
     */
    private Integer promoteStatus;

    /**
     * 投放单价
     */
    private BigDecimal inPrice;

    /**
     * 投放单价
     */
    private BigDecimal outPrice;

    /**
     * 扣量比例
     */
    private Integer deductRate;

    /**
     * 追踪链接
     */
    private String traceUrl;

    /**
     * 追踪类型 ASYNC:异步 REDIRECT:302跳转
     */
    private String traceType;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 编辑者
     */
    private String editor;

    /**
     * 编辑时间
     */
    private LocalDateTime editTime;

    /**
     * 状态
     */
    private Boolean status;

    private String jsonData;


}
