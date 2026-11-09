package com.comeoften.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicCodeSignerTest {

    private static final String SECRET = "unit-test-secret";

    private static final String CODE_SEQ = "RE20261109120000ABCDEF";

    private static final long WINDOW_SECONDS = 30L;

    private static final int SIGN_LENGTH = 10;

    @Test
    void should_match_code_signed_in_previous_window() {
        long signedAt = WINDOW_SECONDS * 1000L + 5L;
        String sign = DynamicCodeSigner.sign("RESERVATION", CODE_SEQ,
                DynamicCodeSigner.window(signedAt, WINDOW_SECONDS), SIGN_LENGTH, SECRET);
        assertThat(DynamicCodeSigner.matches("RESERVATION", CODE_SEQ, sign, signedAt + WINDOW_SECONDS,
                WINDOW_SECONDS, SIGN_LENGTH, SECRET)).isTrue();
    }

    @Test
    void should_reject_code_older_than_previous_window() {
        long signedAt = WINDOW_SECONDS * 1000L + 5L;
        String sign = DynamicCodeSigner.sign("RESERVATION", CODE_SEQ,
                DynamicCodeSigner.window(signedAt, WINDOW_SECONDS), SIGN_LENGTH, SECRET);
        assertThat(DynamicCodeSigner.matches("RESERVATION", CODE_SEQ, sign, signedAt + WINDOW_SECONDS * 2L,
                WINDOW_SECONDS, SIGN_LENGTH, SECRET)).isFalse();
    }

    @Test
    void should_isolate_scopes_with_same_code_seq() {
        long epochSecond = WINDOW_SECONDS * 1000L;
        String couponSign = DynamicCodeSigner.sign("COUPON", CODE_SEQ,
                DynamicCodeSigner.window(epochSecond, WINDOW_SECONDS), SIGN_LENGTH, SECRET);
        assertThat(DynamicCodeSigner.matches("RESERVATION", CODE_SEQ, couponSign, epochSecond,
                WINDOW_SECONDS, SIGN_LENGTH, SECRET)).isFalse();
    }

    @Test
    void should_produce_fixed_length_sign_and_report_remaining_seconds() {
        long epochSecond = WINDOW_SECONDS * 1000L + WINDOW_SECONDS - 3L;
        String code = DynamicCodeSigner.build("RESERVATION", CODE_SEQ, epochSecond, WINDOW_SECONDS,
                SIGN_LENGTH, SECRET);
        assertThat(code).hasSize(CODE_SEQ.length() + SIGN_LENGTH);
        assertThat(code).startsWith(CODE_SEQ);
        assertThat(DynamicCodeSigner.secondsRemaining(epochSecond, WINDOW_SECONDS)).isEqualTo(3L);
    }
}
