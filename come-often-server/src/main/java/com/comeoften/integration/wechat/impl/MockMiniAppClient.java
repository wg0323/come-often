package com.comeoften.integration.wechat.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.config.WxProperties;
import com.comeoften.integration.wechat.MiniAppClient;
import com.comeoften.integration.wechat.WxSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 本地联调实现：同一个 mockKey 始终映射到同一个 openid；
 * 手机号优先取前端手输的 mockPhone，未填时按 phoneCode 派生出稳定号码，可复现「换号换会员」验收。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "comeoften.wx.mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockMiniAppClient implements MiniAppClient {

    private static final String OPENID_PREFIX = "dev_openid_";

    private static final String MOCK_SESSION_KEY = "mock-session-key";

    private static final String MOCK_PHONE_PREFIX = "138";

    private static final int PHONE_TAIL_LENGTH = 8;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final WxProperties wxProperties;

    @Override
    public WxSession code2Session(String code, String mockKey) {
        String seed = StringUtils.hasText(mockKey) ? mockKey : code;
        if (!StringUtils.hasText(seed)) {
            throw new BizException(ErrorCode.WX_CODE_INVALID);
        }
        String digest = md5("openid|" + seed);
        log.debug("Mock code2session: seed={}, openid 已生成", seed);
        return new WxSession(OPENID_PREFIX + digest, null, MOCK_SESSION_KEY);
    }

    @Override
    public String getPhoneNumber(String phoneCode, String mockPhone) {
        if (StringUtils.hasText(mockPhone)) {
            if (!PHONE_PATTERN.matcher(mockPhone).matches()) {
                throw new BizException(ErrorCode.PARAM_INVALID, "手机号格式不正确");
            }
            return mockPhone;
        }
        String seed = StringUtils.hasText(phoneCode) ? phoneCode : "mock-phone";
        String digits = new BigInteger(md5("phone|" + seed), 16).toString();
        return MOCK_PHONE_PREFIX + tailOf(digits, PHONE_TAIL_LENGTH);
    }

    /** 取数字串末 length 位，不足左侧补零，保证拼出的手机号长度稳定。 */
    private String tailOf(String digits, int length) {
        if (digits.length() <= length) {
            StringBuilder padded = new StringBuilder(length);
            for (int i = digits.length(); i < length; i++) {
                padded.append('0');
            }
            return padded.append(digits).toString();
        }
        return digits.substring(digits.length() - length);
    }

    private String md5(String raw) {
        return DigestUtils.md5DigestAsHex(
                (wxProperties.getAppid() + "|" + raw).getBytes(StandardCharsets.UTF_8));
    }
}
