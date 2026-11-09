package com.comeoften.config;

import com.comeoften.security.AdminAuthInterceptor;
import com.comeoften.security.AppAuthInterceptor;
import com.comeoften.security.MerchantAuthInterceptor;
import com.comeoften.security.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 三套拦截器按前缀注册，互不干扰；登录与自动登录接口放行，其余 /api 请求均需对应端的 token。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppAuthInterceptor appAuthInterceptor;

    private final AdminAuthInterceptor adminAuthInterceptor;

    private final MerchantAuthInterceptor merchantAuthInterceptor;

    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(appAuthInterceptor)
                .addPathPatterns("/api/app/**")
                .excludePathPatterns("/api/app/auth/login");
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
        registry.addInterceptor(merchantAuthInterceptor)
                .addPathPatterns("/api/merchant/**")
                .excludePathPatterns("/api/merchant/auth/login", "/api/merchant/auth/auto-login");
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/app/**", "/api/admin/**", "/api/merchant/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
