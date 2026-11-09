package com.comeoften.security;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * 三套拦截器共用的认证逻辑：取 token → 验签与端标识 → 比对 Redis → 复核账号状态 → 写入上下文。
 */
@Component
@RequiredArgsConstructor
public class AuthSupport {

    private static final String AUTH_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;

    private final TokenStoreService tokenStoreService;

    private final AccountStateChecker accountStateChecker;

    public boolean authenticate(HttpServletRequest request, ClientType clientType) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String token = resolveToken(request);
        LoginUser loginUser = jwtTokenService.parse(token, clientType);
        if (!tokenStoreService.isValid(clientType, loginUser.getUserId(), loginUser.getTokenId())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        accountStateChecker.assertActive(loginUser);
        LoginUserHolder.set(loginUser);
        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return token;
    }
}
