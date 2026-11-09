package com.comeoften.module.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 退款审核。通过则订单 REFUNDING→REFUNDED，驳回则 REFUNDING→ACCEPTED 回到制作中。
 */
@Data
@Schema(description = "退款审核请求")
public class RefundAuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "审核结论不能为空")
    @Schema(description = "是否通过退款", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean approved;

    @Size(max = 255, message = "审核说明最多 255 字")
    @Schema(description = "审核说明，写入订单状态日志")
    private String remark;
}
