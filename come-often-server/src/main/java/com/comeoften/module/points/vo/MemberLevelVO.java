package com.comeoften.module.points.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 会员等级配置。门槛与权益全部可由 PC 后台维护，代码不写死档位。
 */
@Data
@Schema(description = "会员等级")
public class MemberLevelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "等级 ID")
    private Long levelId;

    @Schema(description = "等级名称")
    private String levelName;

    @Schema(description = "等级编码")
    private String levelCode;

    @Schema(description = "年度消费额门槛，单位元")
    private BigDecimal minAmount;

    @Schema(description = "累计积分门槛")
    private Integer minPoints;

    @Schema(description = "折扣率，1.00 表示无折扣；M6 仅作权益展示")
    private BigDecimal discountRate;

    @Schema(description = "排序值，越大等级越高")
    private Integer sort;
}
