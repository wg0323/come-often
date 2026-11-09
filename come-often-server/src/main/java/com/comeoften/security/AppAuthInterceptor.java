package com.comeoften.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 顾客小程序 /api/app/** 的身份校验。
 */
@Component
@RequiredArgsConstructor
public class AppAuthInterceptor implements HandlerInterceptor {

    private final AuthSupport authSupport;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return authSupport.authenticate(request, ClientType.APP);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        LoginUserHolder.clear();
    }
}
