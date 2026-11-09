package com.comeoften.security;

/**
 * 三端 JWT 签发与解析，密钥按端隔离，token 内带端标识。
 */
public interface JwtTokenService {

    String issue(ClientType clientType, Long userId, Long storeId);

    LoginUser parse(String token, ClientType expectedClientType);

    long ttlSeconds(ClientType clientType);
}
