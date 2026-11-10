package com.comeoften.module.balance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 储值流水行。顾客端与后台共用，后台多带会员信息时由行外层补齐。
 */
@Data
@Schema(description = "储值流水")
public class BalanceRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "流水 ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "会员 ID")
    private Long memberId;

    @Schema(description = "变动方向：RECHARGE/GIFT/CONSUME/REFUND/ROLLBACK")
    private String changeType;

    @Schema(description = "方向中文说明")
    private String changeTypeText;

    @Schema(description = "变动金额，正数入账负数出账，单位元")
    private BigDecimal changeAmount;

    @Schema(description = "变动后余额，单位元")
    private BigDecimal balanceAfter;

    @Schema(description = "业务类型：RECHARGE/ORDER/REFUND")
    private String bizType;

    @Schema(description = "业务幂等键")
    private String bizId;

    @Schema(description = "说明")
    private String remark;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "发生时间")
    private LocalDateTime createTime;
}
