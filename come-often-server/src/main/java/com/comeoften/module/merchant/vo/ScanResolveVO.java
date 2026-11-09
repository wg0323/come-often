package com.comeoften.module.merchant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 统一扫码结果。成功与失败共用同一个响应体：页面只按 success 上色，
 * 失败时 failReason / failContent 可直接展示，不需要前端再判断码型。
 */
@Data
@Schema(description = "商家端扫码结果")
public class ScanResolveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否核销成功")
    private Boolean success;

    @Schema(description = "码型：COUPON / RESERVATION / PICKUP / UNKNOWN")
    private String codeType;

    @Schema(description = "码型中文说明")
    private String codeTypeText;

    @Schema(description = "业务单号：订单号 / 预约单号 / 用户券 ID")
    private String bizNo;

    @Schema(description = "整屏结果标题")
    private String resultTitle;

    @Schema(description = "整屏结果说明，失败时即为可直接展示的原因")
    private String resultMessage;

    @Schema(description = "失败业务码，与 PC 后台口径一致；成功时不返回")
    private Integer failCode;

    @Schema(description = "失败原因枚举")
    private String failReason;

    @Schema(description = "失败原因中文说明")
    private String failReasonText;

    @Schema(description = "券模板名称")
    private String templateName;

    @Schema(description = "券类型中文说明")
    private String couponTypeText;

    @Schema(description = "券面额，单位元")
    private BigDecimal faceValue;

    @Schema(description = "券使用门槛，单位元")
    private BigDecimal thresholdAmount;

    @Schema(description = "本次抵扣金额，单位元")
    private BigDecimal discountAmount;

    @Schema(description = "预约日期，yyyy-MM-dd")
    private String reserveDate;

    @Schema(description = "预约时段，HH:mm")
    private String timeSlot;

    @Schema(description = "桌型中文说明")
    private String tableTypeText;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "就餐人数")
    private Integer peopleCount;

    @Schema(description = "签到时间，yyyy-MM-dd HH:mm:ss")
    private String checkinTime;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "取餐码 / 到店核销码")
    private String pickupCode;

    @Schema(description = "订单类型中文说明")
    private String orderTypeText;

    @Schema(description = "订单状态中文说明")
    private String orderStatusText;

    @Schema(description = "提示信息，例如提前签到")
    private String notice;
}
