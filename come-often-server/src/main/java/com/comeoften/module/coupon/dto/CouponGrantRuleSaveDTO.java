package com.comeoften.module.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 券发放规则保存请求。rule_code 在同一门店内唯一，重复提交是「编辑」而不是新建。
 */
@Data
@Schema(description = "券发放规则保存请求")
public class CouponGrantRuleSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "规则编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "规则编码只允许大写字母、数字与下划线，2~32 位")
    @Schema(description = "规则编码，同门店唯一", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "BIRTHDAY_2026")
    private String ruleCode;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 64, message = "规则名称最多 64 字")
    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "生日赠券")
    private String ruleName;

    @NotBlank(message = "触发类型不能为空")
    @Schema(description = "触发类型：NEW_MEMBER/BIRTHDAY/REPEAT_PURCHASE/SLEEPING",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "BIRTHDAY")
    private String ruleType;

    @NotNull(message = "券模板不能为空")
    @Schema(description = "发放的券模板 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long couponTemplateId;

    @Min(value = 0, message = "阈值不能为负数")
    @Schema(description = "阈值：复购按已支付订单数（至少 2），沉睡按天数（不填取配置阈值）", example = "2")
    private Integer triggerValue;

    @Min(value = 1, message = "每会员最多发放次数至少 1")
    @Schema(description = "每会员最多发放次数；生日券按年重置，沉睡券按月重置", example = "1")
    private Integer maxPerMember;

    @Schema(description = "是否启用；新建默认 false", example = "true")
    private Boolean enabled;

    @Size(max = 255, message = "说明最多 255 字")
    @Schema(description = "说明")
    private String remark;
}
