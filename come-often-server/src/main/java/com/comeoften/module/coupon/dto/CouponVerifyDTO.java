package com.comeoften.module.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 券核销请求。code 为小程序券码（可带 C 前缀），orderNo 选填：
 * 带订单时按订单金额计算实际抵扣，不带时按券面额记录。
 */
@Data
@Schema(description = "券核销请求")
public class CouponVerifyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请输入券码")
    @Size(max = 32, message = "券码长度不合法")
    @Schema(description = "券码：6 位短序号 + 10 位签名，可带 C 前缀",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "关联订单号；填写时按订单金额计算实际抵扣金额")
    private String orderNo;
}
