package com.comeoften.module.dish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 下单前可售性校验请求。M2 提供接口，M3 的下单主流程复用同一个 Service 断言。
 */
@Data
@Schema(description = "下单前可售性校验请求")
public class OrderCheckDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店 ID，为空回退到当前唯一门店")
    private Long storeId;

    @NotEmpty(message = "校验菜品不能为空")
    @Valid
    @Schema(description = "待校验菜品", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ItemDTO> items;

    /**
     * 待校验菜品行。
     */
    @Data
    @Schema(description = "待校验菜品行")
    public static class ItemDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @NotNull(message = "菜品 ID 不能为空")
        @Schema(description = "菜品 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long dishId;

        @Min(value = 1, message = "份数必须大于 0")
        @Schema(description = "份数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Integer quantity;

        @Schema(description = "已选规格选项 ID")
        private List<Long> specOptionIds;

        @Schema(description = "已选加料 ID")
        private List<Long> addonIds;
    }
}
