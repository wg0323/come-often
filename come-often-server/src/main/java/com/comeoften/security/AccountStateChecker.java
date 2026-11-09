package com.comeoften.security;

/**
 * 每次请求复核账号状态：停用账号或清空微信绑定后，未过期的 token 也要立即失效。
 */
public interface AccountStateChecker {

    void assertActive(LoginUser loginUser);
}
