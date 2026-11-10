package com.comeoften.module.balance.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * PC 后台储值会员行。手机号脱敏口径与后台会员积分页一致。
 */
@Data
@Schema(description = "后台储值会员行")
public class BalanceMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "会员 ID")
    private Long memberId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "脱敏手机号")
    private String phoneMasked;

    @Schema(description = "可用储值余额，单位元")
    private BigDecimal balance;

    @Schema(description = "累计充值本金，单位元")
    private BigDecimal totalRecharge;

    @Schema(description = "累计赠送金额，单位元")
    private BigDecimal totalGift;

    @Schema(description = "累计余额消费，单位元")
    private BigDecimal totalConsume;
}
