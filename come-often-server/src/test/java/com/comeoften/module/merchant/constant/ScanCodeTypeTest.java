package com.comeoften.module.merchant.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScanCodeTypeTest {

    /** 券扫码内容：C + 6 位短序号 + 10 位签名。 */
    private static final String COUPON_CONTENT = "C1234561A2B3C4D5E";

    /** 预约签到码：R + RE + 14 位时间 + 6 位随机 + 10 位签名。 */
    private static final String RESERVATION_CONTENT =
            "RRE20261109190000ABCDEF1A2B3C4D5E";

    @Test
    void should_detect_coupon_when_code_has_c_prefix_and_full_length() {
        assertThat(ScanCodeType.detect(COUPON_CONTENT)).isEqualTo(ScanCodeType.COUPON);
    }

    @Test
    void should_detect_coupon_when_pc_input_omits_prefix() {
        assertThat(ScanCodeType.detect("1234561A2B3C4D5E")).isEqualTo(ScanCodeType.COUPON);
    }

    @Test
    void should_detect_reservation_when_code_starts_with_r() {
        assertThat(ScanCodeType.detect(RESERVATION_CONTENT)).isEqualTo(ScanCodeType.RESERVATION);
    }

    @Test
    void should_detect_pickup_when_code_is_letter_plus_three_digits() {
        assertThat(ScanCodeType.detect("A001")).isEqualTo(ScanCodeType.PICKUP);
        assertThat(ScanCodeType.detect("D999")).isEqualTo(ScanCodeType.PICKUP);
    }

    @Test
    void should_detect_dine_in_codes_in_d_to_g_range() {
        // 到店核销码从 D 段起，当日第 1000 单后进位到 E / F / G
        assertThat(ScanCodeType.detect("E001")).isEqualTo(ScanCodeType.PICKUP);
        assertThat(ScanCodeType.detect("G999")).isEqualTo(ScanCodeType.PICKUP);
    }

    @Test
    void should_return_unknown_when_letter_is_out_of_pickup_range() {
        assertThat(ScanCodeType.detect("P001")).isEqualTo(ScanCodeType.UNKNOWN);
        assertThat(ScanCodeType.detect("H001")).isEqualTo(ScanCodeType.UNKNOWN);
    }

    @Test
    void should_return_unknown_when_code_is_blank_or_short() {
        assertThat(ScanCodeType.detect("")).isEqualTo(ScanCodeType.UNKNOWN);
        assertThat(ScanCodeType.detect(null)).isEqualTo(ScanCodeType.UNKNOWN);
        assertThat(ScanCodeType.detect("R123")).isEqualTo(ScanCodeType.UNKNOWN);
    }

    @Test
    void should_return_unknown_when_reservation_code_length_does_not_match() {
        String truncated = RESERVATION_CONTENT.substring(0, RESERVATION_CONTENT.length() - 5);
        assertThat(ScanCodeType.detect(truncated)).isEqualTo(ScanCodeType.UNKNOWN);
        String padded = RESERVATION_CONTENT + "00";
        assertThat(ScanCodeType.detect(padded)).isEqualTo(ScanCodeType.UNKNOWN);
    }

    @Test
    void should_return_unknown_when_sixteen_chars_are_not_hex() {
        assertThat(ScanCodeType.detect("1234561A2B3C4D5EGH")).isEqualTo(ScanCodeType.UNKNOWN);
    }
}
