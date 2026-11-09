package com.comeoften.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/** 鉴权错误必须输出真实 HTTP 状态码，否则三端 request 封装的 401 分支不会触发。 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void should_return_http_401_when_token_expired() {
        ResponseEntity<Result<Void>> response = handler.handleBizException(new BizException(ErrorCode.TOKEN_EXPIRED));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.TOKEN_EXPIRED.getCode());
    }

    @Test
    void should_return_http_403_when_permission_denied() {
        ResponseEntity<Result<Void>> response = handler.handleBizException(new BizException(ErrorCode.PERMISSION_DENIED));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void should_return_http_200_when_login_failed_with_business_code() {
        ResponseEntity<Result<Void>> response = handler.handleBizException(new BizException(ErrorCode.LOGIN_FAILED));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.LOGIN_FAILED.getCode());
    }
}
