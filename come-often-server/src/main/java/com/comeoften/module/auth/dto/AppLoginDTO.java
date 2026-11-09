package com.comeoften.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/** 顾客端手机号登录请求。手机号是账号唯一标识，openid 仅作最近一次登录记录。 */
@Data
@Schema(description = "顾客端手机号登录请求")
public class AppLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "wx.login 返回的临时凭证，用于记录最近登录的 openid；获取失败不阻塞登录",
            example = "0a1b2c")
    private String code;

    @Schema(description = "getPhoneNumber 回调携带的 code，真实微信环境必填", example = "e3f8...")
    private String phoneCode;

    @Schema(description = "本地 Mock 微信登录的稳定标识；真实微信环境忽略该字段", example = "device-0001")
    private String mockKey;

    @Schema(description = "本地 Mock 环境手输手机号；真实微信环境忽略该字段", example = "13800138000")
    private String mockPhone;
}
