package com.comeoften.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 商家端小程序配置。订阅消息模板与顾客端分属两套小程序，必须各自独立配置：
 * 模板 ID 缺失时新订单提醒降级为落 message_record + 日志（计划 3.10）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "comeoften.merchant")
public class MerchantProperties {

    /** 商家端「新订单提醒」订阅消息模板 ID，comeoften.merchant.subscribe-template-id。 */
    private String subscribeTemplateId;
}
