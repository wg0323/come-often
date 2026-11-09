package com.comeoften.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 订单状态推进请求。目标状态由 OrderStatusTransition 统一校验，非法迁移返回 30002。
 */
@Data
@Schema(description = "订单状态推进请求")
public class OrderTransitionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "目标状态不能为空")
    @Schema(description = "目标状态：PAID/ACCEPTED/READY/COMPLETED/CANCELLED/REFUNDING/REFUNDED",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "ACCEPTED")
    private String toStatus;

    @Size(max = 255, message = "说明最多 255 字")
    @Schema(description = "操作说明")
    private String remark;
}
