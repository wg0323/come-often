package com.comeoften.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 券域配置。券码签名密钥必须由环境变量注入，dev 默认值仅供本地联调。
 */
@Data
@Component
@ConfigurationProperties(prefix = "comeoften.coupon")
public class CouponProperties {

    /** 动态券码 HMAC 密钥，comeoften.coupon.code-secret / COUPON_CODE_SECRET。 */
    private String codeSecret;

    /** 券过期提醒提前量（小时）。 */
    private long expireRemindAheadHours = 24L;

    /**
     * 新人券模板 ID。用字符串接收，允许配置项留空（空串绑定到 Long 会在启动期抛转换异常）；
     * 为空时按 newcomerTemplateName 匹配发放中的同名模板。
     */
    private String newcomerTemplateId;

    /** 新人券模板名称，新人券按名称兜底匹配，避免验收时无法预知雪花 ID。 */
    private String newcomerTemplateName = "新人无门槛券";
}
