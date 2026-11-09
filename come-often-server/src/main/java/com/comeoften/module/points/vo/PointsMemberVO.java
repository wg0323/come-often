package com.comeoften.module.points.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * PC 后台的会员积分行。手机号中间位脱敏，与 M1 的登录信息口径一致。
 */
@Data
@Schema(description = "后台会员积分行")
public class PointsMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "会员 ID")
    private Long memberId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "脱敏手机号")
    private String phoneMasked;

    @Schema(description = "可用积分余额")
    private Integer balance;

    @Schema(description = "累计获得积分")
    private Integer totalEarned;

    @Schema(description = "累计使用积分")
    private Integer totalUsed;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "会员等级 ID")
    private Long levelId;

    @Schema(description = "会员等级编码")
    private String levelCode;

    @Schema(description = "会员等级名称")
    private String levelName;

    @Schema(description = "等级折扣率")
    private BigDecimal discountRate;
}
