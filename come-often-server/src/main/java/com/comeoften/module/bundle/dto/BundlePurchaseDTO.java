package com.comeoften.module.bundle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 购买团购套餐。金额一律服务端按套餐价算，页面只传份数。
 */
@Data
@Schema(description = "购买团购套餐请求")
public class BundlePurchaseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "套餐份数至少 1 份")
    @Max(value = 10, message = "单笔最多购买 10 份套餐")
    @Schema(description = "购买份数，默认 1", example = "1")
    private Integer quantity;
}
