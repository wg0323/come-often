package com.comeoften.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 商家端小程序 /api/merchant/** 的身份校验。
 */
@Component
@RequiredArgsConstructor
public class MerchantAuthInterceptor implements HandlerInterceptor {

    private final AuthSupport authSupport;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return authSupport.authenticate(request, ClientType.MERCHANT);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        LoginUserHolder.clear();
    }
}
