package com.comeoften.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细，全部为下单时快照，菜品后续改名改价不影响历史订单。
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private Long dishId;
    private String dishName;
    private String specDesc;
    private String addonDesc;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal amount;
    private String objectKey;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
