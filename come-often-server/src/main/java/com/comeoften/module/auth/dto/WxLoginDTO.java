package com.comeoften.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/** 小程序登录请求，顾客端登录与商家端免密登录共用。 */
@Data
@Schema(description = "小程序登录请求")
public class WxLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "wx.login 的 code 不能为空")
    @Schema(description = "wx.login 返回的临时凭证", requiredMode = Schema.RequiredMode.REQUIRED, example = "0a1b2c")
    private String code;

    @Schema(description = "本地 Mock 微信登录的稳定标识；真实微信环境忽略该字段", example = "device-0001")
    private String mockKey;
}
