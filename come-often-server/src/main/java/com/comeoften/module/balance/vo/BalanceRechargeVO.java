package com.comeoften.module.balance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 充值结果。充值单号用于对账，赠送额与余额都回给页面，不需要再查一次账户。
 */
@Data
@Schema(description = "充值结果")
public class BalanceRechargeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "充值单号")
    private String rechargeNo;

    @Schema(description = "充值本金，单位元")
    private BigDecimal amount;

    @Schema(description = "赠送金额，单位元；无档位命中为 0")
    private BigDecimal gift;

    @Schema(description = "充值后可用余额，单位元")
    private BigDecimal balance;

    @Schema(description = "支付渠道；当前为 MOCK，真实微信支付属条件任务")
    private String payChannel;
}
