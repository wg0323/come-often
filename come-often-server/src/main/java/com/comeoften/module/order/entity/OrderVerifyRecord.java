package com.comeoften.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单取餐码核销审计。一次订单完成只落一条，靠 uk_biz_idempotent(biz_id) 幂等。
 */
@Data
@TableName("order_verify_record")
public class OrderVerifyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private Long storeId;

    private String verifyCode;

    private String verifyChannel;

    private String operatorType;

    private Long operatorId;

    private LocalDateTime verifyTime;

    /** {orderNo}#PICKUP 或套餐单的 {orderNo}#BUNDLE。 */
    private String bizId;

    private String remark;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
