package com.comeoften.module.coupon.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 券发放规则行。rule_type_text 与「生效周期」文案由服务端拼好，页面只展示。
 */
@Data
@Schema(description = "券发放规则")
public class CouponGrantRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "规则 ID")
    private Long id;

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "触发类型")
    private String ruleType;

    @Schema(description = "触发类型中文说明")
    private String ruleTypeText;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "券模板 ID")
    private Long couponTemplateId;

    @Schema(description = "券模板名称")
    private String templateName;

    @Schema(description = "阈值")
    private Integer triggerValue;

    @Schema(description = "每会员最多发放次数")
    private Integer maxPerMember;

    @Schema(description = "是否启用：0-否，1-是")
    private Integer isEnabled;

    @Schema(description = "规则口径说明，如「每年生日当天发 1 张」")
    private String periodText;

    @Schema(description = "说明")
    private String remark;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
