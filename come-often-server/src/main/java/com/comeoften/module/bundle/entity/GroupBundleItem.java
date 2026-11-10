package com.comeoften.module.bundle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 团购套餐明细。菜名是配置时的快照，下单时再写进 order_item，不引用本表主键。
 */
@Data
@TableName("group_bundle_item")
public class GroupBundleItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long bundleId;

    private Long dishId;

    private String dishName;

    private Integer quantity;

    private Integer sort;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
