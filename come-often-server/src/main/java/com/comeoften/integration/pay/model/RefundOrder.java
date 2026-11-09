package com.comeoften.integration.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 渠道退款入参。refundAmount 是实际已收金额，不是订单整单金额：定金单只收了定金，尾款未收不退。
 */
@Data
@Schema(description = "支付网关退款入参")
public class RefundOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "退款单号")
    private String refundNo;

    @Schema(description = "原商户订单号")
    private String outTradeNo;

    @Schema(description = "原渠道交易号")
    private String tradeNo;

    @Schema(description = "本次退款金额，单位元")
    private BigDecimal refundAmount;

    @Schema(description = "原订单实付金额，单位元")
    private BigDecimal totalAmount;

    @Schema(description = "退款原因")
    private String reason;
}
