package com.comeoften.module.bundle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 团购套餐主表。套餐是独立商品，不套菜品 SPU 模型；M13 的三张团餐券仍是营销券，互不影响。
 */
@Data
@TableName("group_bundle")
public class GroupBundle implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long storeId;

    private String bundleName;

    private String description;

    private BigDecimal bundlePrice;

    private BigDecimal originalPrice;

    private Integer totalStock;

    /** 剩余库存，DB 条件更新是并发扣减的最终事实源。 */
    private Integer remainStock;

    private Integer status;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
