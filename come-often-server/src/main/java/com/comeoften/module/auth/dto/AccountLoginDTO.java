package com.comeoften.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/** PC 后台员工账号登录请求。 */
@Data
@Schema(description = "员工账号登录请求")
public class AccountLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "登录账号不能为空")
    @Schema(description = "登录账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6 到 64 位之间")
    @Schema(description = "登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "Come@2026")
    private String password;
}
