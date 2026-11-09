package com.comeoften.integration.storage.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.config.MinioProperties;
import com.comeoften.integration.storage.FileStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * MinIO 实现。wx.uploadFile 仅支持 POST，无法用预签名 PUT 直传，故由后端接收 multipart 转存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {

    private static final long PART_SIZE = 5L * 1024 * 1024;

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public String upload(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "文件不能为空");
        }
        if (!ALLOWED_PREFIX.contains(prefix)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "对象前缀只允许 dish / banner / review");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String extension = dot < 0 ? "" : original.substring(dot);
        String objectKey = prefix + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
        return put(file, objectKey);
    }

    @Override
    public String uploadWithKey(MultipartFile file, String objectKey) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "文件不能为空");
        }
        if (!FileStorageService.isAllowedObjectKey(objectKey)) {
            throw new BizException(ErrorCode.FILE_OBJECT_KEY_INVALID);
        }
        return put(file, objectKey);
    }

    @Override
    public boolean exists(String objectKey) {
        if (!FileStorageService.isAllowedObjectKey(objectKey)) {
            return false;
        }
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            // 对象不存在是业务事实，按 false 返回；桶不可用等异常仍要冒泡成系统错误
            log.info("对象不存在: objectKey={}, code={}", objectKey, e.errorResponse() == null
                    ? "" : e.errorResponse().code());
            return false;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "对象存储不可用", e);
        }
    }

    @Override
    public byte[] get(String objectKey) {
        if (!FileStorageService.isAllowedObjectKey(objectKey)) {
            throw new BizException(ErrorCode.FILE_OBJECT_KEY_INVALID);
        }
        try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectKey)
                .build())) {
            return StreamUtils.copyToByteArray(in);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "文件读取失败", e);
        }
    }

    private String put(MultipartFile file, String objectKey) {
        ensureBucket();
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), PART_SIZE)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "文件上传失败", e);
        }
        log.info("文件已上传: objectKey={}, size={}", objectKey, file.getSize());
        return objectKey;
    }

    private void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
                log.info("已创建私有桶: {}", properties.getBucket());
            }
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "MinIO 桶不可用", e);
        }
    }
}
