package com.comeoften.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单状态日志，每次状态迁移写一条。
 */
@Data
@TableName("order_status_log")
public class OrderStatusLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private String fromStatus;
    private String toStatus;
    private String operatorType;
    private Long operatorId;
    private String remark;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
