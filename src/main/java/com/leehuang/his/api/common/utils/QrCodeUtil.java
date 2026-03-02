package com.leehuang.his.api.common.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class QrCodeUtil {

    /**
     * 生成二维码 base64 字符串（不带 data:image 前缀）
     * @param content       二维码内容
     * @param width         二维码宽度
     * @param height        二维码高度
     * @return
     */
    public static String generateBase64(String content, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1); // 边距

            BitMatrix bitMatrix = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);

            byte[] bytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);

        } catch (Exception e) {
            throw new RuntimeException("生成二维码失败", e);
        }
    }

    /**
     * 生成二维码 Base64（带 data:image/png;base64 前缀，前端可直接用）
     */
    public static String generateBase64WithPrefix(String content, int width, int height) {
        return "data:image/png;base64," + generateBase64(content, width, height);
    }
}
