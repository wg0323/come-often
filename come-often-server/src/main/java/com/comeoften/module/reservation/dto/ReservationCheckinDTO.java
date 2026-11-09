package com.comeoften.module.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 扫码签到。code 为 R 前缀的签到码，服务端解析后校验签名与时间窗。
 */
@Data
@Schema(description = "预约签到请求")
public class ReservationCheckinDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "签到码不能为空")
    @Size(max = 64, message = "签到码长度不合法")
    @Schema(description = "签到码，R 前缀 + 预约单号 + 10 位签名", required = true)
    private String code;

    @Schema(description = "绑定的具体桌台 ID，可空；为空时保留原绑定")
    private Long tableId;
}
