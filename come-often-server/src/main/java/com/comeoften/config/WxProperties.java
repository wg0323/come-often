package com.comeoften.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 小程序配置。开发期 mock-enabled=true 时不访问微信服务器，也不需要 AppSecret。
 */
@Data
@Component
@ConfigurationProperties(prefix = "comeoften.wx")
public class WxProperties {

    private String appid;

    private String secret;

    private String merchantAppid;

    private String merchantSecret;

    private boolean mockEnabled;

    public String effectiveMerchantAppid() {
        return StringUtils.hasText(merchantAppid) ? merchantAppid : appid;
    }

    public String effectiveMerchantSecret() {
        return StringUtils.hasText(merchantSecret) ? merchantSecret : secret;
    }
}
