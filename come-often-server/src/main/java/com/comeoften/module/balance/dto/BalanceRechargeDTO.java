package com.comeoften.module.balance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 充值请求。只传金额：赠送额由服务端按档位算，页面不参与。
 */
@Data
@Schema(description = "储值充值请求")
public class BalanceRechargeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于 0")
    @Schema(description = "充值金额，单位元，最多两位小数", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "500.00")
    private BigDecimal amount;
}
