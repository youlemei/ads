package com.lwz.ads.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 点击记录, 按日分表
 * </p>
 *
 * @author lwz
 * @since 2020-02-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class ClickRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * IP
     */
    private String ip;

    /**
     * 设备id iOS:IDFA Android:IMEI
     */
    private String mac;

    /**
     * 参数json
     */
    private String paramJson;

    /**
     * 点击状态 0.未转化 1.已转化 2.已核减
     */
    private Integer clickStatus;

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
