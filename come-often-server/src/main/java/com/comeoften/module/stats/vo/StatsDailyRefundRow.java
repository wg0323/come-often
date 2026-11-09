package com.comeoften.module.stats.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 内部聚合行：按原订单支付自然日的已退款金额。退款按 paid_time 冲减，不按 refund_time。
 */
@Data
public class StatsDailyRefundRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate statDate;

    private BigDecimal refundAmount;
}
