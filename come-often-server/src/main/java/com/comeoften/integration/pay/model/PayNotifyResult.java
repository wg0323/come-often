package com.comeoften.integration.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 渠道回调解析结果。outTradeNo 是回调幂等唯一键，payAmount 必须等于服务端本次应收。
 */
@Data
@Schema(description = "支付回调解析结果")
public class PayNotifyResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商户订单号")
    private String outTradeNo;

    @Schema(description = "渠道交易号")
    private String tradeNo;

    @Schema(description = "渠道实收金额，单位元")
    private BigDecimal payAmount;

    @Schema(description = "渠道标识")
    private String channel;

    @Schema(description = "是否支付成功")
    private Boolean success;

    @Schema(description = "渠道收款时间")
    private LocalDateTime paidAt;
}
