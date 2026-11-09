package com.comeoften.module.merchant.constant;

import com.comeoften.module.coupon.constant.CouponConstants;
import com.comeoften.module.order.util.PickupCodeGenerator;
import com.comeoften.module.reservation.constant.ReservationConstants;

/**
 * 商家端扫码分流码型。判据只认真实码串形态，不引入计划 3.12 里并不存在的 P 前缀：
 * <ul>
 *     <li>券码：C + 6 位短序号 + 10 位签名（PC 手动输码允许省略 C，16 位纯十六进制）</li>
 *     <li>预约签到码：R + 预约单号（RE + 14 位时间 + 6 位随机）+ 10 位签名</li>
 *     <li>取餐 / 到店核销码：A001 起的 1 位字母 + 3 位数字，字母区间直接取 {@link PickupCodeGenerator}</li>
 * </ul>
 */
public enum ScanCodeType {

    COUPON("券码"),

    RESERVATION("预约签到码"),

    PICKUP("取餐码"),

    UNKNOWN("未知码");

    /** 券码不带前缀时的长度：短序号 + 签名。 */
    private static final int COUPON_LENGTH = CouponConstants.CODE_SEQ_LENGTH + CouponConstants.SIGN_LENGTH;

    /** 券码带 C 前缀时的长度。 */
    private static final int COUPON_CONTENT_LENGTH = COUPON_LENGTH + CouponConstants.CODE_PREFIX.length();

    /** 签到码带 R 前缀时的总长度。 */
    private static final int RESERVATION_LENGTH = ReservationConstants.CODE_PREFIX.length()
            + ReservationConstants.RESERVE_NO_LENGTH + ReservationConstants.SIGN_LENGTH;

    private final String description;

    ScanCodeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 按码串形态判定码型。入参必须是已规范化的码串（去空格、转大写）。
     */
    public static ScanCodeType detect(String code) {
        if (code == null || code.isEmpty()) {
            return UNKNOWN;
        }
        if (PickupCodeGenerator.matches(code)) {
            return PICKUP;
        }
        if (code.startsWith(CouponConstants.CODE_PREFIX) && code.length() == COUPON_CONTENT_LENGTH) {
            return COUPON;
        }
        if (code.startsWith(ReservationConstants.CODE_PREFIX) && code.length() == RESERVATION_LENGTH) {
            return RESERVATION;
        }
        if (code.length() == COUPON_LENGTH && isHex(code)) {
            // PC 输码允许省略 C 前缀，商家端扫到同类纯码串时按券处理
            return COUPON;
        }
        return UNKNOWN;
    }

    private static boolean isHex(String code) {
        for (int index = 0; index < code.length(); index++) {
            char value = code.charAt(index);
            boolean digit = value >= '0' && value <= '9';
            boolean letter = value >= 'A' && value <= 'F';
            if (!digit && !letter) {
                return false;
            }
        }
        return true;
    }
}
