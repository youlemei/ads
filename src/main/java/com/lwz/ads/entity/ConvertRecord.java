package com.lwz.ads.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 转化记录
 * </p>
 *
 * @author lwz
 * @since 2020-02-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class ConvertRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 点击id
     */
    private String clickId;

    /**
     * 点击时间
     */
    private LocalDateTime clickTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

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
     * 回调url
     */
    private String callback;

    /**
     * 转化状态 0.收到 1.已转化 2.已通知渠道 3.已核减
     */
    private Integer convertStatus;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 编辑时间
     */
    private LocalDateTime editTime;

    /**
     * 编辑标记
     */
    private String editor;

    /**
     * 状态
     */
    private Boolean status;

    private String jsonData;


}
