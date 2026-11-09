package com.comeoften.integration.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 对象键白名单校验，防止公开读取通道被用作任意对象遍历。
 */
class FileStorageServiceTest {

    @Test
    void should_accept_dish_webp_key_when_pipeline_names_it() {
        assertThat(FileStorageService.isAllowedObjectKey("dish/1001_list.webp")).isTrue();
        assertThat(FileStorageService.isAllowedObjectKey("banner/9001.webp")).isTrue();
        assertThat(FileStorageService.isAllowedObjectKey("review/1.png")).isTrue();
    }

    @Test
    void should_reject_when_key_has_path_traversal() {
        assertThat(FileStorageService.isAllowedObjectKey("dish/../secret.txt")).isFalse();
        assertThat(FileStorageService.isAllowedObjectKey("dish\\..\\secret.txt")).isFalse();
        assertThat(FileStorageService.isAllowedObjectKey("/dish/1001_list.webp")).isFalse();
    }

    @Test
    void should_reject_when_prefix_is_not_whitelisted() {
        assertThat(FileStorageService.isAllowedObjectKey("private/seed.sql")).isFalse();
        assertThat(FileStorageService.isAllowedObjectKey("")).isFalse();
        assertThat(FileStorageService.isAllowedObjectKey(null)).isFalse();
        assertThat(FileStorageService.isAllowedObjectKey("dish")).isFalse();
    }
}
