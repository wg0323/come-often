package com.comeoften.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 仅在关闭 Mock 时创建微信 SDK 客户端，dev 默认不依赖任何真实资质。
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "comeoften.wx.mock-enabled", havingValue = "false")
public class WxMaConfig {

    private final WxProperties wxProperties;

    @Bean
    public WxMaService wxMaService() {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(wxProperties.getAppid());
        config.setSecret(wxProperties.getSecret());
        WxMaServiceImpl service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }
}
