package com.comeoften.security;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.auth.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 方法级 RBAC 校验，只作用于标注 @RequirePerm 的接口。
 */
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        RequirePerm requirePerm = ((HandlerMethod) handler).getMethodAnnotation(RequirePerm.class);
        if (requirePerm == null) {
            return true;
        }
        LoginUser loginUser = LoginUserHolder.getRequired();
        if (!permissionService.listPermissions(loginUser.getUserId()).contains(requirePerm.value())) {
            throw new BizException(ErrorCode.PERMISSION_DENIED, requirePerm.value());
        }
        return true;
    }
}
