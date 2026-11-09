package com.comeoften.module.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 修改购物车行数量。
 */
@Data
@Schema(description = "修改购物车数量请求")
public class CartQuantityDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "份数不能为空")
    @Min(value = 1, message = "份数必须大于 0，删除请用 DELETE")
    @Schema(description = "份数", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer quantity;
}
