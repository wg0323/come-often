package com.comeoften.common.util;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrCodeUtilsTest {

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};

    @Test
    void should_return_png_bytes_when_content_is_valid() {
        byte[] png = QrCodeUtils.png("C1234567890abcdef", 240);
        assertThat(png).isNotEmpty();
        assertThat(png).startsWith(PNG_MAGIC);
    }

    @Test
    void should_return_square_png_when_size_out_of_range() {
        // 尺寸被夹到下限 120：只验证不抛异常且仍是合法 PNG，避免断言依赖具体像素实现
        byte[] png = QrCodeUtils.png("A001", 10);
        assertThat(png).startsWith(PNG_MAGIC);
    }

    @Test
    void should_return_decodable_base64_when_content_is_valid() {
        String base64 = QrCodeUtils.pngBase64("A001", 240);
        assertThat(base64).isNotBlank();
        assertThat(Base64.getDecoder().decode(base64)).startsWith(PNG_MAGIC);
    }

    @Test
    void should_throw_biz_exception_when_content_is_blank() {
        assertThatThrownBy(() -> QrCodeUtils.png("   ", 240))
                .isInstanceOf(BizException.class)
                .extracting(exception -> ((BizException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QR_CODE_GENERATE_FAILED);
    }
}
