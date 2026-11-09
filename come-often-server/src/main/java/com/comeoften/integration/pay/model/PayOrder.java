package com.comeoften.integration.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 预下单入参。金额一律由服务端订单快照给出，渠道不参与计算。
 */
@Data
@Schema(description = "支付网关预下单入参")
public class PayOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商户订单号")
    private String outTradeNo;

    @Schema(description = "本次应收金额，单位元；定金单为定金")
    private BigDecimal payAmount;

    @Schema(description = "商品简述")
    private String subject;

    @Schema(description = "顾客 openid；Mock 模式可为空")
    private String openid;
}
