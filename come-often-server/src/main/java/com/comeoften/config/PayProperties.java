package com.comeoften.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付配置。dev 默认走 Mock；切到 wxpay 需补齐商户号、证书序列号、APIv3 密钥与私钥路径。
 */
@Data
@Component
@ConfigurationProperties(prefix = "comeoften.pay")
public class PayProperties {

    private static final String DEFAULT_GATEWAY = "mock";

    private String gateway = DEFAULT_GATEWAY;

    private WxPay wxpay = new WxPay();

    /**
     * 微信支付 V3 凭据。M4 只留配置项与骨架，不启用、不引入 SDK。
     */
    @Data
    public static class WxPay {

        private String mchId;

        private String certSerialNo;

        private String apiV3Key;

        private String privateKeyPath;
    }
}
