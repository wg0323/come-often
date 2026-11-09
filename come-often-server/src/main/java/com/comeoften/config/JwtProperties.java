package com.comeoften.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 三端 JWT 配置：各自独立密钥与有效期，密钥不同即无法跨端伪造。
 */
@Data
@Component
@ConfigurationProperties(prefix = "comeoften.jwt")
public class JwtProperties {

    private String issuer;

    private String appSecret;

    private String merchantSecret;

    private String adminSecret;

    private long appTtlSeconds;

    private long merchantTtlSeconds;

    private long adminTtlSeconds;
}
