package com.comeoften.integration.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 预下单结果。syncPaid 为 true 表示网关自身已同步确认收款（Mock），业务侧可直接走入账回调；
 * 为 false 表示等渠道异步通知（微信支付 V3）。
 */
@Data
@Schema(description = "支付网关预下单结果")
public class PrepayResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "网关标识：MOCK/WXPAY_V3")
    private String gateway;

    @Schema(description = "商户订单号")
    private String outTradeNo;

    @Schema(description = "渠道交易号")
    private String tradeNo;

    @Schema(description = "预支付会话标识")
    private String prepayId;

    @Schema(description = "本次应收金额，单位元")
    private BigDecimal payAmount;

    @Schema(description = "是否已同步确认收款")
    private Boolean syncPaid;
}
