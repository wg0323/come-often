package com.comeoften.integration.wechat.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.config.WxProperties;
import com.comeoften.integration.wechat.WxSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Mock 微信登录：同一设备标识始终映射到同一 openid，可复现绑定与踢登验收。 */
class MockMiniAppClientTest {

    private final MockMiniAppClient client = new MockMiniAppClient(properties());

    @Test
    void should_return_same_openid_when_mock_key_is_stable() {
        WxSession first = client.code2Session("code-a", "device-0001");
        WxSession second = client.code2Session("code-b", "device-0001");

        assertThat(second.getOpenid()).isEqualTo(first.getOpenid());
        assertThat(first.getOpenid()).hasSizeLessThan(64);
    }

    @Test
    void should_return_different_openid_when_mock_key_differs() {
        WxSession first = client.code2Session("code-a", "device-0001");
        WxSession second = client.code2Session("code-a", "device-0002");

        assertThat(second.getOpenid()).isNotEqualTo(first.getOpenid());
    }

    @Test
    void should_fall_back_to_code_when_mock_key_is_blank() {
        WxSession session = client.code2Session("code-a", "");

        assertThat(session.getOpenid()).isNotBlank();
    }

    @Test
    void should_throw_when_both_code_and_mock_key_are_blank() {
        assertThatExceptionOfType(BizException.class)
                .isThrownBy(() -> client.code2Session("", null))
                .satisfies(e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WX_CODE_INVALID));
    }

    private WxProperties properties() {
        WxProperties properties = new WxProperties();
        properties.setAppid("wx-test-appid");
        properties.setMockEnabled(true);
        return properties;
    }
}
