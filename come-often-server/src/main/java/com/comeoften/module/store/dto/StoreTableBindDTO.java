package com.comeoften.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 桌码绑定请求。顾客扫到桌码后由服务端校验并回桌台 ID，绑定关系不落库：
 * 一次就餐有效，下单时把桌台 ID 带进订单，订单之外不需要任何服务端状态。
 */
@Data
@Schema(description = "桌码绑定请求")
public class StoreTableBindDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "桌码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,20}$", message = "桌码格式不合法")
    @Schema(description = "桌码，即桌台表里的桌号；二维码里带 code= / t= 参数时由前端先解析",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "A01")
    private String tableCode;
}
