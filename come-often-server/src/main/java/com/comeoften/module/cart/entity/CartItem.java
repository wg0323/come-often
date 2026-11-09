package com.comeoften.module.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 购物车行。规格与加料以排序后的 ID 组合键存储，保证相同 SKU 只占一行。
 */
@Data
@TableName("cart_item")
public class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long memberId;
    private Long storeId;
    private Long dishId;
    private String specKey;
    private String addonKeys;
    private Integer quantity;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
