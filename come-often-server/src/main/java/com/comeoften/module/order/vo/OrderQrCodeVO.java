package com.comeoften.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 取餐码 / 到店核销码的二维码。码串本身就是订单的 {@code pickup_code}（A001 / D001 起），
 * 商家端扫码后由 {@code MerchantScanService} 按码型分流，不需要额外前缀或签名。
 */
@Data
@Schema(description = "取餐码二维码")
public class OrderQrCodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "取餐码 / 到店核销码")
    private String pickupCode;

    @Schema(description = "二维码 PNG 的 base64（不带 data URI 前缀）；生成失败时为 null")
    private String qrImageBase64;
}
