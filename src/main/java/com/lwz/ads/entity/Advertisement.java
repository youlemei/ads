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
 * 广告
 * </p>
 *
 * @author lwz
 * @since 2020-02-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Advertisement implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 广告主id
     */
    private Long companyId;

    /**
     * 类型 1.直链广告
     */
    private Integer adType;

    /**
     * 广告
     */
    private String adName;

    /**
     * 类别
     */
    private Integer adCategory;

    /**
     * 系统 1.IOS 2.Android
     */
    private Integer systemType;

    /**
     * 截止时间
     */
    private LocalDateTime endTime;

    /**
     * 推广开关
     */
    private Boolean traceStatus;

    /**
     * 结算类型 1.CPA
     */
    private Integer settleType;

    /**
     * 接入单价
     */
    private BigDecimal inPrice;

    /**
     * 追踪方式 ASYNC:异步 REDIRECT:302跳转
     */
    private String traceType;

    /**
     * 预览链接
     */
    private String previewUrl;

    /**
     * 广告追踪链接
     */
    private String traceUrl;

    /**
     * 去重策略
     */
    private Integer deduplicateType;

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
