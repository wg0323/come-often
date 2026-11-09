package com.comeoften.module.system.controller;

import com.comeoften.common.Result;
import com.comeoften.integration.storage.FileStorageService;
import com.comeoften.module.system.vo.FileVO;
import com.comeoften.security.RequirePerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口。M0 用它验证 MinIO 上传后能取回，M2 图片管线接同一 Service。
 */
@Tag(name = "文件管理", description = "菜品图、轮播图、评价图的上传与取回")
@RestController
@RequestMapping("/api/admin/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传文件", description = "后端接收 multipart 转存 MinIO；prefix 只允许 dish / banner / review")
    @ApiResponse(responseCode = "200", description = "上传成功，返回 objectKey")
    @PostMapping("/upload")
    public Result<FileVO> upload(
            @Parameter(description = "待上传文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "对象前缀：dish / banner / review", required = true)
            @RequestParam("prefix") String prefix) {
        FileVO vo = new FileVO();
        vo.setObjectKey(fileStorageService.upload(file, prefix));
        vo.setSize(file.getSize());
        return Result.ok(vo);
    }

    @Operation(summary = "按指定 objectKey 上传", description = "供 M2 图片管线复现 dish/{dishId}_{list|detail|thumb}.webp，重复上传覆盖同名对象")
    @ApiResponse(responseCode = "200", description = "上传成功，返回与入参一致的 objectKey")
    @RequirePerm("file:upload")
    @PostMapping("/upload-with-key")
    public Result<FileVO> uploadWithKey(
            @Parameter(description = "待上传文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "指定对象键，前缀只允许 dish / banner / review", required = true)
            @RequestParam("objectKey") String objectKey) {
        FileVO vo = new FileVO();
        vo.setObjectKey(fileStorageService.uploadWithKey(file, objectKey));
        vo.setSize(file.getSize());
        return Result.ok(vo);
    }

    @Operation(summary = "取回文件", description = "按 objectKey 返回原始字节，用于验证对象可读")
    @ApiResponse(responseCode = "200", description = "返回文件字节")
    @GetMapping
    public ResponseEntity<byte[]> get(
            @Parameter(description = "上传时返回的对象键", required = true)
            @RequestParam("objectKey") String objectKey) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileStorageService.get(objectKey));
    }
}
