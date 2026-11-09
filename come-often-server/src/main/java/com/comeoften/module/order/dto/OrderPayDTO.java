package com.comeoften.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 发起支付请求。M4 起由 PayGateway 预下单 + 支付成功回调入账；金额只用于校验，服务端不接受前端计算值。
 */
@Data
@Schema(description = "模拟支付请求")
public class OrderPayDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "本次支付金额；含预定菜应等于 200 元定金，普通单应等于实付金额", example = "200.00")
    private BigDecimal payAmount;
}
