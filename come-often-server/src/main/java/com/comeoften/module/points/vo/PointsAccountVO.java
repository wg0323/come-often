package com.comeoften.module.points.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 积分账户概览。余额与累计值全部来自 points_account，等级来自 member_level，
 * 页面只展示不计算。
 */
@Data
@Schema(description = "积分账户概览")
public class PointsAccountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "会员 ID")
    private Long memberId;

    @Schema(description = "可用积分余额")
    private Integer balance;

    @Schema(description = "累计获得积分")
    private Integer totalEarned;

    @Schema(description = "累计使用积分")
    private Integer totalUsed;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "当前会员等级 ID")
    private Long levelId;

    @Schema(description = "当前会员等级编码，如 SILVER")
    private String levelCode;

    @Schema(description = "当前会员等级名称，如 银卡会员")
    private String levelName;

    @Schema(description = "当前等级折扣率，1.00 表示无折扣；M6 仅作权益展示")
    private BigDecimal discountRate;

    @Schema(description = "本次操作获得的积分；仅签到 / 分享 / 完善资料返回，其余为 0")
    private Integer earnedPoints = 0;

    @Schema(description = "今日是否已签到")
    private Boolean signedToday;

    @Schema(description = "今日是否已领过分享积分")
    private Boolean sharedToday;
}
