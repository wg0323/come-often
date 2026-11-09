package com.comeoften.module.points.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 积分规则展示。规则参数来自 application.yml 的 comeoften.points.*，
 * M6 不新增配置表，PC 后台只读展示（见 deploy/init.md 差异）。
 */
@Data
@Schema(description = "积分规则")
public class PointsRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "每消费 1 元获得的积分")
    private Integer orderEarnRate;

    @Schema(description = "多少积分抵 1 元")
    private Integer exchangeRate;

    @Schema(description = "每日签到积分")
    private Integer signInPoints;

    @Schema(description = "评价积分，M8 接入")
    private Integer reviewPoints;

    @Schema(description = "每日分享积分")
    private Integer sharePoints;

    @Schema(description = "完善资料积分，终身一次")
    private Integer profilePoints;

    @Schema(description = "积分有效期天数")
    private Integer validDays;

    @Schema(description = "抵现说明文案")
    private String exchangeText;
}
