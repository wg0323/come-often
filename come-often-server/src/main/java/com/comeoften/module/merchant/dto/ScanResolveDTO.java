package com.comeoften.module.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 商家端统一扫码请求。前端只传扫到的原始内容，不做码型判断与二次确认。
 */
@Data
@Schema(description = "商家端统一扫码请求")
public class ScanResolveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "扫码内容不能为空")
    @Size(max = 64, message = "扫码内容长度不合法")
    @Schema(description = "扫码内容：券码（C + 16 位）/ 预约签到码（R + 单号 + 10 位签名）/ 取餐码（4 位）原样传入",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "C1234561A2B3C4D5E")
    private String code;
}
