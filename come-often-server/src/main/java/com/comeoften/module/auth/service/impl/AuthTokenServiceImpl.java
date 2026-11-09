package com.comeoften.module.auth.service.impl;

import com.comeoften.module.auth.service.AuthTokenService;
import com.comeoften.module.auth.vo.LoginVO;
import com.comeoften.security.ClientType;
import com.comeoften.security.JwtTokenService;
import com.comeoften.security.LoginUser;
import com.comeoften.security.TokenStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 签发 JWT 并把 jti 写入 Redis，二者 TTL 一致。 */
@Service
@RequiredArgsConstructor
public class AuthTokenServiceImpl implements AuthTokenService {

    private final JwtTokenService jwtTokenService;

    private final TokenStoreService tokenStoreService;

    @Override
    public LoginVO issue(ClientType clientType, Long userId, Long storeId) {
        String token = jwtTokenService.issue(clientType, userId, storeId);
        LoginUser issued = jwtTokenService.parse(token, clientType);
        long ttl = jwtTokenService.ttlSeconds(clientType);
        tokenStoreService.save(clientType, userId, issued.getTokenId(), ttl);
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpiresIn(ttl);
        vo.setClientType(clientType.name());
        vo.setUserId(userId);
        vo.setStoreId(storeId);
        return vo;
    }

    @Override
    public void logout(LoginUser loginUser) {
        tokenStoreService.remove(loginUser.getClientType(), loginUser.getUserId());
    }
}
