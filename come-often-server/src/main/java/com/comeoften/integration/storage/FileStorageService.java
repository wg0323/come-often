package com.comeoften.integration.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 文件存储抽象。DB 只存 objectKey，不存完整 URL，换域名不需迁移数据。
 */
public interface FileStorageService {

    List<String> ALLOWED_PREFIX = Arrays.asList("dish", "banner", "review");

    /**
     * 上传文件并返回自动生成的 objectKey。
     */
    String upload(MultipartFile file, String prefix);

    /**
     * 以指定 objectKey 上传，供图片管线复现 dish/{dishId}_{list|detail|thumb}.webp 命名。
     */
    String uploadWithKey(MultipartFile file, String objectKey);

    /**
     * 按 objectKey 取回文件内容。
     */
    byte[] get(String objectKey);

    /**
     * 对象是否存在。评价提交前用它拦住不存在的 objectKey，避免库里留下永远打不开的图。
     */
    boolean exists(String objectKey);

    /**
     * 对象键是否属于允许读写的前缀，且不含路径穿越。公开读取与指定键上传共用同一份校验。
     */
    static boolean isAllowedObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isEmpty() || objectKey.length() > 200
                || objectKey.startsWith("/") || objectKey.contains("..") || objectKey.contains("\\")) {
            return false;
        }
        int slash = objectKey.indexOf('/');
        return slash > 0 && ALLOWED_PREFIX.contains(objectKey.substring(0, slash));
    }
}
