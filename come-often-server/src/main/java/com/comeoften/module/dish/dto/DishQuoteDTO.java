package com.comeoften.module.dish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 金额试算请求。页面只提交已选项，金额由服务端计算后返回。
 */
@Data
@Schema(description = "菜品金额试算请求")
public class DishQuoteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "菜品 ID 不能为空")
    @Schema(description = "菜品 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dishId;

    @Min(value = 1, message = "份数必须大于 0")
    @Schema(description = "份数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer quantity;

    @Schema(description = "已选规格选项 ID（每组至多一项）")
    private List<Long> specOptionIds;

    @Schema(description = "已选加料 ID")
    private List<Long> addonIds;

    @Schema(description = "门店 ID，由服务端按当前门店覆盖，前端无需传")
    private Long storeId;

    @Schema(description = "会员 ID，由服务端按登录态写入，前端不传；非空时才应用会员价",
            hidden = true)
    private Long memberId;
}
