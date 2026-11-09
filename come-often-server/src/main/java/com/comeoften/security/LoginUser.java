package com.comeoften.security;

import lombok.Data;

import java.io.Serializable;

/**
 * 一次请求内的登录上下文，由认证拦截器写入 ThreadLocal。
 */
@Data
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private ClientType clientType;

    private Long userId;

    /** 员工所属门店；顾客端为 null。 */
    private Long storeId;

    /** JWT 的 jti，用于与 Redis 中的当前有效凭证比对，实现主动踢登。 */
    private String tokenId;
}
