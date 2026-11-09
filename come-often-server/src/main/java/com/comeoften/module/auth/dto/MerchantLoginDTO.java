package com.comeoften.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/** 商家端首次登录：员工账号密码 + 微信 code，成功后写入 sys_user.wx_openid。 */
@Data
@Schema(description = "商家端首次登录请求")
public class MerchantLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "登录账号不能为空")
    @Schema(description = "登录账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "manager")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6 到 64 位之间")
    @Schema(description = "登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "Come@2026")
    private String password;

    @NotBlank(message = "wx.login 的 code 不能为空")
    @Schema(description = "wx.login 返回的临时凭证，用于绑定微信", requiredMode = Schema.RequiredMode.REQUIRED, example = "0a1b2c")
    private String code;

    @Schema(description = "本地 Mock 微信登录的稳定标识；真实微信环境忽略该字段", example = "device-1001")
    private String mockKey;
}
