package com.comeoften.module.coupon.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 核销记录。operatorName 由 sys_user 关联查出，供 PC 后台展示「谁核销了哪张券」。
 */
@Data
@Schema(description = "券核销记录")
public class CouponUseRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "核销记录 ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "用户券 ID")
    private Long userCouponId;

    @Schema(description = "券模板名称")
    private String templateName;

    @Schema(description = "券类型中文说明")
    private String couponTypeText;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "持券会员 ID")
    private Long memberId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "关联订单 ID")
    private Long orderId;

    @Schema(description = "实际抵扣金额，单位元")
    private BigDecimal discountAmount;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "核销员工 ID")
    private Long operatorUserId;

    @Schema(description = "核销员工姓名")
    private String operatorName;

    @Schema(description = "核销入口：ADMIN_INPUT/MERCHANT_SCAN")
    private String verifyChannel;

    @Schema(description = "核销入口中文说明")
    private String verifyChannelText;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "核销时间")
    private LocalDateTime verifyTime;
}
