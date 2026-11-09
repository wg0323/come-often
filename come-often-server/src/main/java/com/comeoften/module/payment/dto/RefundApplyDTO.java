package com.comeoften.module.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 退款申请。金额一律由服务端按已收流水计算，请求体不接受金额。
 */
@Data
@Schema(description = "退款申请请求")
public class RefundApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 255, message = "退款原因最多 255 字")
    @Schema(description = "退款原因", example = "临时有事无法到店")
    private String reason;
}
