package com.lwz.ads.mapper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 广告主
 * </p>
 *
 * @author lwz
 * @since 2020-02-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Company implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 广告主名
     */
    private String companyName;

    /**
     * 邮箱
     */
    private String companyMailbox;

    /**
     * 类型
     */
    private Integer companyType;

    /**
     * 官网
     */
    private String companyWeb;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系人邮箱
     */
    private String contactMailbox;

    /**
     * 手机号码
     */
    private String contactPhoneNumber;

    /**
     * QQ
     */
    private String contactQq;

    /**
     * 微信
     */
    private String contactWechat;

    /**
     * 职位
     */
    private String contactPosition;

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
