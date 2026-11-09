package com.comeoften.security.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.config.JwtProperties;
import com.comeoften.security.ClientType;
import com.comeoften.security.LoginUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** JWT 签发、验签、过期与跨端隔离的纯逻辑测试，不依赖 Spring 上下文与中间件。 */
class JwtTokenServiceImplTest {

    private static final long ONE_DAY_SECONDS = 86400L;

    private static final String APP_SECRET = "test-app-secret-that-is-longer-than-32-bytes";

    private static final String MERCHANT_SECRET = "test-merchant-secret-that-is-longer-than-32";

    private static final String ADMIN_SECRET = "test-admin-secret-that-is-longer-than-32b";

    @Test
    void should_parse_issued_token_when_same_client_type() {
        JwtTokenServiceImpl service = serviceWithDistinctSecrets(ONE_DAY_SECONDS);
        String token = service.issue(ClientType.APP, 123L, null);

        LoginUser loginUser = service.parse(token, ClientType.APP);

        assertThat(loginUser.getUserId()).isEqualTo(123L);
        assertThat(loginUser.getClientType()).isEqualTo(ClientType.APP);
        assertThat(loginUser.getStoreId()).isNull();
        assertThat(loginUser.getTokenId()).isNotBlank();
    }

    @Test
    void should_keep_store_id_when_staff_token_issued() {
        JwtTokenServiceImpl service = serviceWithDistinctSecrets(ONE_DAY_SECONDS);
        String token = service.issue(ClientType.ADMIN, 7L, 1L);

        LoginUser loginUser = service.parse(token, ClientType.ADMIN);

        assertThat(loginUser.getStoreId()).isEqualTo(1L);
    }

    @Test
    void should_reject_token_when_client_type_mismatch() {
        JwtProperties properties = properties(ONE_DAY_SECONDS);
        JwtTokenServiceImpl service = new JwtTokenServiceImpl(properties);
        String token = service.issue(ClientType.APP, 123L, null);

        assertThatExceptionOfType(BizException.class)
                .isThrownBy(() -> service.parse(token, ClientType.MERCHANT))
                .satisfies(e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOKEN_CLIENT_MISMATCH));
    }

    @Test
    void should_reject_app_token_when_used_on_merchant_with_distinct_secret() {
        JwtTokenServiceImpl service = serviceWithDistinctSecrets(ONE_DAY_SECONDS);
        String token = service.issue(ClientType.APP, 123L, null);

        assertThatExceptionOfType(BizException.class)
                .isThrownBy(() -> service.parse(token, ClientType.MERCHANT))
                .satisfies(e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    @Test
    void should_throw_expired_when_token_already_expired() {
        JwtTokenServiceImpl service = serviceWithDistinctSecrets(-10L);
        String token = service.issue(ClientType.APP, 123L, null);

        assertThatExceptionOfType(BizException.class)
                .isThrownBy(() -> service.parse(token, ClientType.APP))
                .satisfies(e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOKEN_EXPIRED));
    }

    @Test
    void should_throw_invalid_when_token_tampered() {
        JwtTokenServiceImpl service = serviceWithDistinctSecrets(ONE_DAY_SECONDS);
        String token = service.issue(ClientType.APP, 123L, null);
        String suffix = token.substring(token.length() - 2);
        String tampered = token.substring(0, token.length() - 2) + ("AA".equals(suffix) ? "BB" : "AA");

        assertThatExceptionOfType(BizException.class)
                .isThrownBy(() -> service.parse(tampered, ClientType.APP))
                .satisfies(e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    @Test
    void should_return_configured_ttl_for_each_client_type() {
        JwtTokenServiceImpl service = serviceWithDistinctSecrets(ONE_DAY_SECONDS);

        assertThat(service.ttlSeconds(ClientType.APP)).isEqualTo(ONE_DAY_SECONDS);
        assertThat(service.ttlSeconds(ClientType.MERCHANT)).isEqualTo(43200L);
        assertThat(service.ttlSeconds(ClientType.ADMIN)).isEqualTo(7200L);
    }

    private JwtTokenServiceImpl serviceWithDistinctSecrets(long appTtlSeconds) {
        JwtProperties properties = properties(appTtlSeconds);
        properties.setAppSecret(APP_SECRET);
        properties.setMerchantSecret(MERCHANT_SECRET);
        properties.setAdminSecret(ADMIN_SECRET);
        return new JwtTokenServiceImpl(properties);
    }

    private JwtProperties properties(long appTtlSeconds) {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("come-often-test");
        properties.setAppSecret(APP_SECRET);
        properties.setMerchantSecret(APP_SECRET);
        properties.setAdminSecret(APP_SECRET);
        properties.setAppTtlSeconds(appTtlSeconds);
        properties.setMerchantTtlSeconds(43200L);
        properties.setAdminTtlSeconds(7200L);
        return properties;
    }
}
