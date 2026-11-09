package com.comeoften.module.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 商家端一键售罄 / 开售请求。备货总量由服务端按当前库存补齐，页面不传数字。
 */
@Data
@Schema(description = "商家端售罄切换请求")
public class SoldOutToggleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "菜品 ID 不能为空")
    @Schema(description = "菜品 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dishId;

    @NotNull(message = "售罄标记不能为空")
    @Schema(description = "true 售罄，false 开售", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "true")
    private Boolean soldOut;
}
