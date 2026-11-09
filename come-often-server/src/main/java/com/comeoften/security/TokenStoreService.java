package com.comeoften.security;

/**
 * Redis 中的当前有效凭证。TTL 与 JWT 一致，删除即踢登。
 */
public interface TokenStoreService {

    void save(ClientType clientType, Long userId, String tokenId, long ttlSeconds);

    boolean isValid(ClientType clientType, Long userId, String tokenId);

    void remove(ClientType clientType, Long userId);
}
