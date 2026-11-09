package com.comeoften.security.impl;

import com.comeoften.security.ClientType;
import com.comeoften.security.TokenStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * key 统一为 comeoften:auth:{端}:token:{userId}，与共用 Redis 中其它项目的 key 隔离。
 */
@Service
@RequiredArgsConstructor
public class TokenStoreServiceImpl implements TokenStoreService {

    private static final String KEY_PREFIX = "comeoften:auth:";

    private static final String KEY_SUFFIX = ":token:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(ClientType clientType, Long userId, String tokenId, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(keyOf(clientType, userId), tokenId, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isValid(ClientType clientType, Long userId, String tokenId) {
        String stored = stringRedisTemplate.opsForValue().get(keyOf(clientType, userId));
        return StringUtils.hasText(stored) && stored.equals(tokenId);
    }

    @Override
    public void remove(ClientType clientType, Long userId) {
        stringRedisTemplate.delete(keyOf(clientType, userId));
    }

    private String keyOf(ClientType clientType, Long userId) {
        return KEY_PREFIX + clientType.getPrefix() + KEY_SUFFIX + userId;
    }
}
