package com.comeoften.module.merchant.constant;

import com.comeoften.common.ErrorCode;

/**
 * 扫码核销的失败原因。每个原因绑定既有业务码，前端只需展示 reasonText，
 * 排查时同时能拿到与 PC 后台一致的业务码。
 */
public enum ScanFailReason {

    COUPON_NOT_FOUND(ErrorCode.COUPON_NOT_FOUND),

    COUPON_USED(ErrorCode.COUPON_ALREADY_USED),

    COUPON_EXPIRED(ErrorCode.COUPON_EXPIRED),

    COUPON_CODE_INVALID(ErrorCode.COUPON_CODE_INVALID),

    COUPON_STORE_MISMATCH(ErrorCode.COUPON_STORE_MISMATCH),

    COUPON_TEMPLATE_NOT_FOUND(ErrorCode.COUPON_TEMPLATE_NOT_FOUND),

    COUPON_THRESHOLD_NOT_ENOUGH(ErrorCode.COUPON_THRESHOLD_NOT_ENOUGH),

    RESERVATION_NOT_FOUND(ErrorCode.RESERVATION_NOT_FOUND),

    RESERVATION_CODE_INVALID(ErrorCode.RESERVATION_CODE_INVALID),

    RESERVATION_STATUS_INVALID(ErrorCode.RESERVATION_STATUS_INVALID),

    RESERVATION_ACCESS_DENIED(ErrorCode.RESERVATION_ACCESS_DENIED),

    ORDER_NOT_FOUND(ErrorCode.ORDER_NOT_FOUND),

    ORDER_NOT_READY(ErrorCode.ORDER_STATUS_INVALID, "餐品尚未出餐，未到取餐时间"),

    ORDER_COMPLETED(ErrorCode.ORDER_STATUS_INVALID, "该订单已完成取餐"),

    ORDER_STATUS_INVALID(ErrorCode.ORDER_STATUS_INVALID),

    ORDER_ACCESS_DENIED(ErrorCode.ORDER_ACCESS_DENIED),

    CODE_UNRECOGNIZED(ErrorCode.PARAM_INVALID, "无法识别的码，请确认是券码 / 签到码 / 取餐码"),

    SYSTEM_ERROR(ErrorCode.SYSTEM_ERROR);

    private final ErrorCode errorCode;

    private final String defaultText;

    ScanFailReason(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.defaultText = errorCode.getMessage();
    }

    ScanFailReason(ErrorCode errorCode, String defaultText) {
        this.errorCode = errorCode;
        this.defaultText = defaultText;
    }

    public Integer getCode() {
        return errorCode.getCode();
    }

    public String getDefaultText() {
        return defaultText;
    }

    /**
     * 把业务错误码翻译成失败原因；未列入的原因统一归为系统错误。
     */
    public static ScanFailReason of(ErrorCode errorCode) {
        if (errorCode == null) {
            return SYSTEM_ERROR;
        }
        for (ScanFailReason reason : values()) {
            if (reason.errorCode == errorCode && reason != ORDER_NOT_READY && reason != ORDER_COMPLETED
                    && reason != CODE_UNRECOGNIZED) {
                return reason;
            }
        }
        return SYSTEM_ERROR;
    }
}
