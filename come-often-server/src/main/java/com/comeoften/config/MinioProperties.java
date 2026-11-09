package com.comeoften.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 连接参数，凭据通过环境变量注入。
 */
@Data
@Component
@ConfigurationProperties(prefix = "comeoften.minio")
public class MinioProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
}
