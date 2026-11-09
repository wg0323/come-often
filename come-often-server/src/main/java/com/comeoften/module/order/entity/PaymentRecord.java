package com.comeoften.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水。out_trade_no 唯一索引保证同一订单只成功入账一次。
 */
@Data
@TableName("payment_record")
public class PaymentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String outTradeNo;
    private String tradeNo;
    private Long orderId;
    private BigDecimal payAmount;
    private String payChannel;
    private Integer status;
    private LocalDateTime notifyTime;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
