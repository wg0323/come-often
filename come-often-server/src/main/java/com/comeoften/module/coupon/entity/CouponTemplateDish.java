package com.comeoften.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 券模板适用菜品关联。单品券可指定多道菜，coupon_template.dish_id 保留单菜快照口径。
 */
@Data
@TableName("coupon_template_dish")
public class CouponTemplateDish implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long templateId;
    private Long dishId;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
