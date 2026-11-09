package com.comeoften.module.coupon.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 核销记录里出现过的员工，供商家端「按员工筛选」使用。
 */
@Data
@Schema(description = "核销员工")
public class CouponOperatorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "员工 ID")
    private Long operatorUserId;

    @Schema(description = "员工姓名")
    private String operatorName;
}
