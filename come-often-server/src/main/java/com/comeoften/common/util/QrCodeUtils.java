package com.comeoften.common.util;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

/**
 * 二维码出图（M14 B2）。只依赖 zxing core，PNG 用 ImageIO 自绘，不引 javase 模块。
 *
 * <p>为什么出图放后端：券码 / 取餐码的码串必须由服务端按时间窗现算（券码 30 秒一变），
 * 端上只负责显示；后端出图后小程序用 {@code <image src="data:image/png;base64,...">} 直接渲染，
 * 不必在三个端各维护一份 canvas 绘制实现。</p>
 *
 * <p>线程安全：无共享可变状态，方法都是静态纯函数。</p>
 */
public final class QrCodeUtils {

    private static final String IMAGE_FORMAT = "png";

    private static final String CHARSET = "UTF-8";

    /**
     * 静默区（白边）模块数。取 1 而不是默认 4：小程序卡片里二维码尺寸有限，
     * 白边过大会把有效像素挤小，影响扫码成功率。
     */
    private static final int MARGIN_MODULES = 1;

    private static final int MIN_SIZE = 120;

    private static final int MAX_SIZE = 1024;

    private static final int BLACK = 0xFF000000;

    private static final int WHITE = 0xFFFFFFFF;

    private QrCodeUtils() {
    }

    /**
     * 生成二维码 PNG 字节。
     *
     * @param content 码内容，不能为空
     * @param size    图片边长（像素），超出 120~1024 会被夹到边界值
     */
    public static byte[] png(String content, int size) {
        if (content == null || content.trim().isEmpty()) {
            throw new BizException(ErrorCode.QR_CODE_GENERATE_FAILED);
        }
        int side = Math.min(MAX_SIZE, Math.max(MIN_SIZE, size));
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, CHARSET);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, MARGIN_MODULES);
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, side, side, hints);
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, IMAGE_FORMAT, output);
            return output.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new BizException(ErrorCode.QR_CODE_GENERATE_FAILED);
        }
    }

    /**
     * 生成二维码并转 base64（不带 data URI 前缀，由端上拼接）。
     */
    public static String pngBase64(String content, int size) {
        return Base64.getEncoder().encodeToString(png(content, size));
    }
}
