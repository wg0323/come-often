package com.comeoften.module.coupon.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 动态券码。code 每 30 秒变化一次，codeContent 供小程序直接渲染二维码/条码。
 */
@Data
@Schema(description = "动态券码")
public class CouponCodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "用户券 ID")
    private Long userCouponId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "券模板 ID")
    private Long templateId;

    @Schema(description = "券模板名称")
    private String templateName;

    @Schema(description = "券类型中文说明")
    private String couponTypeText;

    @Schema(description = "面额展示文案")
    private String faceText;

    @Schema(description = "使用条件文案")
    private String conditionText;

    @Schema(description = "券码：6 位短序号 + 10 位签名")
    private String code;

    @Schema(description = "扫码内容，带 C 前缀供商家端扫码分流")
    private String codeContent;

    @Schema(description = "二维码 PNG 的 base64（不带 data URI 前缀）；生成失败时为 null，端上降级只显示数字券码")
    private String qrImageBase64;

    @Schema(description = "本窗口剩余秒数，页面按此设置刷新定时器")
    private Long secondsRemaining;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "券失效时间")
    private LocalDateTime expireAt;

    @Schema(description = "面额，单位元")
    private BigDecimal faceValue;
}
