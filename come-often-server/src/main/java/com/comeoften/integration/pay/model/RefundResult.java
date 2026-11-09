package com.comeoften.integration.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 渠道退款结果。
 */
@Data
@Schema(description = "支付网关退款结果")
public class RefundResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "退款单号")
    private String refundNo;

    @Schema(description = "渠道退款单号")
    private String channelRefundNo;

    @Schema(description = "渠道是否受理成功")
    private Boolean success;
}
