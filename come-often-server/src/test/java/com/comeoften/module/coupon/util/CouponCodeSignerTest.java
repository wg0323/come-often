package com.comeoften.module.coupon.util;

import com.comeoften.module.coupon.constant.CouponConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CouponCodeSignerTest {

    private static final String SECRET = "unit-test-secret";

    private static final String CODE_SEQ = "123456";

    private static final long WINDOW_SECOND = CouponConstants.TIME_WINDOW_SECONDS;

    @Test
    void should_match_code_signed_in_current_window() {
        long epochSecond = WINDOW_SECOND * 1000L + 5L;
        String sign = CouponCodeSigner.sign(CODE_SEQ, CouponCodeSigner.window(epochSecond), SECRET);
        assertThat(CouponCodeSigner.matches(CODE_SEQ, sign, epochSecond, SECRET)).isTrue();
    }

    @Test
    void should_still_match_when_code_signed_in_previous_window() {
        long signedAt = WINDOW_SECOND * 1000L + 5L;
        String sign = CouponCodeSigner.sign(CODE_SEQ, CouponCodeSigner.window(signedAt), SECRET);
        long checkedAt = signedAt + WINDOW_SECOND;
        assertThat(CouponCodeSigner.matches(CODE_SEQ, sign, checkedAt, SECRET)).isTrue();
    }

    @Test
    void should_reject_code_when_older_than_previous_window() {
        long signedAt = WINDOW_SECOND * 1000L + 5L;
        String sign = CouponCodeSigner.sign(CODE_SEQ, CouponCodeSigner.window(signedAt), SECRET);
        long checkedAt = signedAt + WINDOW_SECOND * 2L;
        assertThat(CouponCodeSigner.matches(CODE_SEQ, sign, checkedAt, SECRET)).isFalse();
    }

    @Test
    void should_reject_when_sign_does_not_match_code_seq() {
        long epochSecond = WINDOW_SECOND * 1000L + 5L;
        String sign = CouponCodeSigner.sign("654321", CouponCodeSigner.window(epochSecond), SECRET);
        assertThat(CouponCodeSigner.matches(CODE_SEQ, sign, epochSecond, SECRET)).isFalse();
    }

    @Test
    void should_reject_when_secret_differs() {
        long epochSecond = WINDOW_SECOND * 1000L + 5L;
        String sign = CouponCodeSigner.sign(CODE_SEQ, CouponCodeSigner.window(epochSecond), SECRET);
        assertThat(CouponCodeSigner.matches(CODE_SEQ, sign, epochSecond, "another-secret")).isFalse();
    }

    @Test
    void should_produce_ten_char_sign_and_sixteen_char_code() {
        long epochSecond = WINDOW_SECOND * 1000L;
        String code = CouponCodeSigner.buildCode(CODE_SEQ, epochSecond, SECRET);
        assertThat(code).hasSize(CouponConstants.CODE_SEQ_LENGTH + CouponConstants.SIGN_LENGTH);
        assertThat(code).startsWith(CODE_SEQ);
    }

    @Test
    void should_change_code_when_time_window_advances() {
        long epochSecond = WINDOW_SECOND * 1000L;
        String current = CouponCodeSigner.buildCode(CODE_SEQ, epochSecond, SECRET);
        String next = CouponCodeSigner.buildCode(CODE_SEQ, epochSecond + WINDOW_SECOND, SECRET);
        assertThat(current).isNotEqualTo(next);
    }

    @Test
    void should_report_remaining_seconds_inside_current_window() {
        long epochSecond = WINDOW_SECOND * 1000L + WINDOW_SECOND - 3L;
        assertThat(CouponCodeSigner.secondsRemaining(epochSecond)).isEqualTo(3L);
    }
}
