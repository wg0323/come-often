package com.comeoften.module.system.controller;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.integration.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 小程序 image 组件不能带 Bearer 头，故提供只读图片通道。
 * 路径 /api/public/** 不在三端鉴权拦截器注册范围内；仅允许白名单前缀，杜绝任意对象读取。
 */
@Tag(name = "公开图片", description = "只读图片通道，供小程序 image 组件与 PC 后台缩略图引用")
@RestController
@RequestMapping("/api/public/file")
@RequiredArgsConstructor
public class PublicFileController {

    private static final long CACHE_DAYS = 7L;

    private final FileStorageService fileStorageService;

    @Operation(summary = "读取图片", description = "按 objectKey 返回字节，仅 dish / banner / review 前缀可用，无需 token")
    @ApiResponse(responseCode = "200", description = "返回图片字节，Content-Type 按扩展名推断")
    @ApiResponse(responseCode = "200", description = "对象键非法时 body.code=20009")
    @GetMapping
    public ResponseEntity<byte[]> get(
            @Parameter(description = "对象键，如 dish/1001_list.webp", required = true)
            @RequestParam("objectKey") String objectKey) {
        if (!FileStorageService.isAllowedObjectKey(objectKey)) {
            throw new BizException(ErrorCode.FILE_OBJECT_KEY_INVALID);
        }
        return ResponseEntity.ok()
                .contentType(resolveMediaType(objectKey))
                .cacheControl(CacheControl.maxAge(CACHE_DAYS, TimeUnit.DAYS))
                .body(fileStorageService.get(objectKey));
    }

    private MediaType resolveMediaType(String objectKey) {
        String lower = objectKey.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
