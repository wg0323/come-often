package com.comeoften.security;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;

/**
 * 登录上下文持有者。拦截器在 afterCompletion 必须 clear，避免线程池复用串号。
 */
public final class LoginUserHolder {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginUserHolder() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser getRequired() {
        LoginUser loginUser = HOLDER.get();
        if (loginUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
