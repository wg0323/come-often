package com.comeoften.module.auth.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/** 当前登录顾客信息，手机号脱敏返回。 */
@Data
@Schema(description = "当前登录顾客信息")
public class MemberInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "会员 ID")
    private Long memberId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 objectKey 或 URL")
    private String avatar;

    @Schema(description = "手机号，中间四位脱敏")
    private String phone;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "会员等级 ID")
    private Long levelId;
}
