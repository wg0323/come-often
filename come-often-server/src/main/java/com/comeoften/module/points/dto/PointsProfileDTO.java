package com.comeoften.module.points.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 完善资料。手机号已是登录账号唯一标识，只能通过微信手机号授权变更，
 * 因此本接口只更新昵称，避免用户手改手机号导致换号或唯一键冲突。
 */
@Data
@Schema(description = "完善资料请求")
public class PointsProfileDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称最多 64 字")
    @Schema(description = "昵称", example = "老王", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;
}
