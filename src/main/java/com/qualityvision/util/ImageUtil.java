package com.qualityvision.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 图片预处理：VLM 按 token 计费且对大图延迟高，
 * 上传图统一压缩到长边 ≤1024px 并转 JPEG，兼顾精度与成本。
 */
public class ImageUtil {

    private static final int MAX_SIDE = 1024;

    public static byte[] compressToJpeg(byte[] raw) throws IOException {
        BufferedImage src = ImageIO.read(new java.io.ByteArrayInputStream(raw));
        if (src == null) {
            throw new IOException("无法解析图片格式");
        }
        int w = src.getWidth(), h = src.getHeight();
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(w, h));
        BufferedImage out;
        if (scale < 1.0) {
            int nw = (int) (w * scale), nh = (int) (h * scale);
            out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, nw, nh, null);
            g.dispose();
        } else {
            // 小图也转 RGB，保证 JPEG 编码不丢 alpha 通道
            out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(src, 0, 0, null);
            g.dispose();
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(out, "jpg", bos);
        return bos.toByteArray();
    }
}
