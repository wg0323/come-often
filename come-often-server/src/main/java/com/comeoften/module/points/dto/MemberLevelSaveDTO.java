package com.comeoften.module.points.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 等级配置编辑。只允许改门槛与折扣率，等级编码是幂等键与日志口径，不可改。
 */
@Data
@Schema(description = "会员等级配置")
public class MemberLevelSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "等级名称不能为空")
    @Size(max = 32, message = "等级名称最多 32 字")
    @Schema(description = "等级名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "银卡会员")
    private String levelName;

    @NotNull(message = "年度消费额门槛不能为空")
    @DecimalMin(value = "0", message = "年度消费额门槛不能为负")
    @Digits(integer = 8, fraction = 2, message = "年度消费额最多两位小数")
    @Schema(description = "年度消费额门槛，单位元", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1000.00")
    private BigDecimal minAmount;

    @NotNull(message = "累计积分门槛不能为空")
    @Min(value = 0, message = "累计积分门槛不能为负")
    @Schema(description = "累计积分门槛", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    private Integer minPoints;

    @NotNull(message = "折扣率不能为空")
    @DecimalMin(value = "0.01", message = "折扣率必须大于 0")
    @DecimalMax(value = "1.00", message = "折扣率不能超过 1.00")
    @Digits(integer = 1, fraction = 2, message = "折扣率最多两位小数")
    @Schema(description = "折扣率，1.00 表示无折扣", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "0.95")
    private BigDecimal discountRate;

    @Min(value = 0, message = "排序值不能为负")
    @Schema(description = "排序值，越大等级越高；不传保持原值", example = "2")
    private Integer sort;
}
