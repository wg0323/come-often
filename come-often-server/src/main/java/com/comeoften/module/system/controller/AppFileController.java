package com.comeoften.module.system.controller;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.Result;
import com.comeoften.integration.storage.FileStorageService;
import com.comeoften.module.system.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 顾客端上传。会员只允许上传评价图，禁止借该入口覆盖菜品图或轮播图；
 * 只收图片类型，避免把桶当网盘用。
 */
@Tag(name = "顾客端-文件", description = "评价图上传，DB 只存 objectKey")
@RestController
@RequestMapping("/api/app/file")
@RequiredArgsConstructor
public class AppFileController {

    private static final String REVIEW_PREFIX = "review";

    private static final long MAX_REVIEW_IMAGE_BYTES = 3L * 1024 * 1024;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传评价图", description = "prefix 只接受 review；只允许 jpg / jpeg / png / webp")
    @ApiResponse(responseCode = "200", description = "上传成功，返回 objectKey")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileVO> upload(
            @Parameter(description = "待上传图片", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "对象前缀，只接受 review", required = true)
            @RequestParam("prefix") String prefix) {
        if (!REVIEW_PREFIX.equals(prefix)) {
            throw new BizException(ErrorCode.FILE_OBJECT_KEY_INVALID, "顾客端仅支持上传评价图");
        }
        assertImage(file);
        FileVO vo = new FileVO();
        vo.setObjectKey(fileStorageService.upload(file, prefix));
        vo.setSize(file.getSize());
        return Result.ok(vo);
    }

    private void assertImage(MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String extension = dot < 0 ? "" : original.substring(dot).toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension) || !contentType.startsWith("image/")) {
            throw new BizException(ErrorCode.PARAM_INVALID, "只支持 jpg / jpeg / png / webp 图片");
        }
        // 全局 multipart 上限是 20MB（容纳菜品原图），评价图单独收紧，避免把桶当网盘
        if (file.getSize() > MAX_REVIEW_IMAGE_BYTES) {
            throw new BizException(ErrorCode.PARAM_INVALID, "评价图不能超过 3MB");
        }
    }
}
