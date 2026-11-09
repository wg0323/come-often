package com.comeoften.module.auth.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/** 登录结果。雪花 ID 一律序列化为字符串，避免 JS 精度丢失。 */
@Data
@Schema(description = "登录结果")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "JWT，后续请求放入 Authorization: Bearer <token>")
    private String token;

    @Schema(description = "有效期，单位秒", example = "86400")
    private Long expiresIn;

    @Schema(description = "登录端标识：APP / MERCHANT / ADMIN")
    private String clientType;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "会员或员工 ID")
    private Long userId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "所属门店 ID；顾客端为 null")
    private Long storeId;
}
