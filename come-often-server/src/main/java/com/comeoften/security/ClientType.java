package com.comeoften.security;

/**
 * 登录端标识。JWT 的 typ 声明、独立密钥、Redis key 前缀都按此区分，跨端盗用直接被拒。
 */
public enum ClientType {

    APP("app"),
    MERCHANT("merchant"),
    ADMIN("admin");

    private final String prefix;

    ClientType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
