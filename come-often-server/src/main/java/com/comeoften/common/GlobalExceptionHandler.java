package com.comeoften.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理，不向前端暴露系统内部细节。鉴权类异常同时输出 HTTP 401/403，供三端 request 封装识别。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity.status(resolveHttpStatus(e.getErrorCode()))
                .body(Result.fail(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValidationException(BindException e) {
        String message = e.getBindingResult().getFieldError() == null
                ? ErrorCode.PARAM_INVALID.getMessage()
                : e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity.ok(Result.fail(ErrorCode.PARAM_INVALID, message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Result<Void>> handleMalformedRequest(Exception e) {
        log.warn("请求参数无法解析: {}", e.getMessage());
        return ResponseEntity.ok(Result.fail(ErrorCode.PARAM_INVALID));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(ErrorCode.PARAM_INVALID, "请求方法不支持"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.ok(Result.fail(ErrorCode.SYSTEM_ERROR));
    }

    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        switch (errorCode) {
            case UNAUTHORIZED:
            case TOKEN_EXPIRED:
            case TOKEN_INVALID:
            case TOKEN_CLIENT_MISMATCH:
                return HttpStatus.UNAUTHORIZED;
            case FORBIDDEN:
            case PERMISSION_DENIED:
            case STORE_UNBOUND:
                return HttpStatus.FORBIDDEN;
            default:
                return HttpStatus.OK;
        }
    }
}
