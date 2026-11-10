package com.comeoften.module.balance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 顾客端储值账户概览。档位一并下发，充值页不写死任何金额。
 */
@Data
@Schema(description = "储值账户概览")
public class BalanceAccountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "可用储值余额，单位元")
    private BigDecimal balance;

    @Schema(description = "累计充值本金，单位元")
    private BigDecimal totalRecharge;

    @Schema(description = "累计赠送金额，单位元")
    private BigDecimal totalGift;

    @Schema(description = "累计余额消费，单位元")
    private BigDecimal totalConsume;

    @Schema(description = "单笔充值下限，单位元")
    private BigDecimal minRecharge;

    @Schema(description = "单笔充值上限，单位元")
    private BigDecimal maxRecharge;

    @Schema(description = "充值赠送档位，按门槛升序")
    private List<GiftTierVO> giftTiers;

    /**
     * 赠送档位。
     */
    @Data
    @Schema(description = "充值赠送档位")
    public static class GiftTierVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "充值门槛，单位元")
        private BigDecimal amount;

        @Schema(description = "赠送金额，单位元")
        private BigDecimal gift;

        @Schema(description = "展示文案，如「充 500 送 50」")
        private String text;
    }
}
