package com.comeoften.security.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.config.JwtProperties;
import com.comeoften.security.ClientType;
import com.comeoften.security.JwtTokenService;
import com.comeoften.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * HS256 JWT。过期、签名错误、端标识不符分别给出不同错误码，前端据此决定续登还是回到登录页。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {

    private static final String CLAIM_CLIENT_TYPE = "typ";

    private static final String CLAIM_STORE_ID = "sid";

    private static final long MILLIS_PER_SECOND = 1000L;

    private final JwtProperties jwtProperties;

    @Override
    public String issue(ClientType clientType, Long userId, Long storeId) {
        long ttl = ttlSeconds(clientType);
        Date now = new Date();
        return Jwts.builder()
                .setIssuer(jwtProperties.getIssuer())
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_CLIENT_TYPE, clientType.name())
                .claim(CLAIM_STORE_ID, storeId)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ttl * MILLIS_PER_SECOND))
                .signWith(secretKeyOf(clientType), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public LoginUser parse(String token, ClientType expectedClientType) {
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(secretKeyOf(expectedClientType))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        String actualType = claims.get(CLAIM_CLIENT_TYPE, String.class);
        if (!expectedClientType.name().equals(actualType)) {
            throw new BizException(ErrorCode.TOKEN_CLIENT_MISMATCH);
        }
        Long storeId = claims.get(CLAIM_STORE_ID, Long.class);
        LoginUser loginUser = new LoginUser();
        loginUser.setClientType(expectedClientType);
        loginUser.setUserId(Long.valueOf(claims.getSubject()));
        loginUser.setStoreId(storeId);
        loginUser.setTokenId(claims.getId());
        return loginUser;
    }

    @Override
    public long ttlSeconds(ClientType clientType) {
        switch (clientType) {
            case APP:
                return jwtProperties.getAppTtlSeconds();
            case MERCHANT:
                return jwtProperties.getMerchantTtlSeconds();
            case ADMIN:
                return jwtProperties.getAdminTtlSeconds();
            default:
                throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private SecretKey secretKeyOf(ClientType clientType) {
        switch (clientType) {
            case APP:
                return Keys.hmacShaKeyFor(jwtProperties.getAppSecret().getBytes(StandardCharsets.UTF_8));
            case MERCHANT:
                return Keys.hmacShaKeyFor(jwtProperties.getMerchantSecret().getBytes(StandardCharsets.UTF_8));
            case ADMIN:
                return Keys.hmacShaKeyFor(jwtProperties.getAdminSecret().getBytes(StandardCharsets.UTF_8));
            default:
                throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
