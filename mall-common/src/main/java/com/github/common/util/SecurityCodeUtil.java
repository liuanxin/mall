package com.github.common.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/** 生成验证码 */
public final class SecurityCodeUtil {

    private static final Random RANDOM = new Random();

    /** 验证码库(英文) */
    private static final String WORD = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    /** 验证码库(数字) */
    private static final String NUMBER = "0123456789";
    /** 验证码库(数字 + 英文, 不包括小写 l、大写 I、小写 o 和 大写 O, 避免跟数字 1 和 0 相似) */
    private static final String WORD_NUMBER = "0123456789abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    /** 字体 */
    private static final String[] FONTS = new String[] {
            "consola",
            "monospace",
            "monaco",
            "Verdana",
            "Helvetica",
            "arial",
            "serif",
            "sans-serif",
            "Times",
            "fixedsys"
    };

    /**
     * <pre>
     * 生成验证码图像对象
     *
     * SecurityCodeUtil.Code code = generateCode(count, style, width, height, rgb);
     *
     * // 往 session 里面丢值
     * session.setAttribute("xxx", code.getContent());
     *
     * // 向页面渲染图像
     * response.setDateHeader("Expires", 0);
     * response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
     * response.addHeader("Cache-Control", "post-check=0, pre-check=0");
     * response.setHeader("Pragma", "no-cache");
     * response.setContentType("image/png");
     * javax.imageio.ImageIO.write(code.getImage(), "png", response.getOutputStream());
     * </pre>
     *
     * @param count  验证码数字个数, 最少 4 个. 传空值或传小于 4 的值会使用最少值
     * @param style  图片上的文字: 英文(w)、数字(n), 否则是数字 + 英文(不包括小写 l、大写 I、小写 o 和 大写 O)
     * @param width  生成的图片宽度, 最小 100. 传空值或传小于 100 的值会使用最小值
     * @param height 生成的图片高度, 最小 30. 传空值或传小于 30 的值会使用最小值
     * @param rgba 生成的图片上面验证码的颜色, 不传则默认是 57,66,108,1
     * @return 图像
     */
    public static Code generateCode(String count, String style, String width, String height, String rgba) {
        int loop = Math.max(toInt(count), 4);
        int widthCount = Math.max(toInt(width), 100);
        int heightCount = Math.max(toInt(height), 30);
        // 默认是数字 + 英文
        String str = "w".equalsIgnoreCase(style) ? WORD : ("n".equalsIgnoreCase(style) ? NUMBER : WORD_NUMBER);

        int r = -1, g = -1, b = -1, a = -1;
        if (U.isNotBlank(rgba)) {
            String[] s = rgba.split(",");
            r = U.toInt(s[0].trim());
            int len = s.length;
            if (len > 1) {
                g = U.toInt(s[1].trim());
                if (len > 2) {
                    b = U.toInt(s[2].trim());
                }
                if (len > 3) {
                    // alpha 在 0 ~ 1 之间, 就乘以 255, 大于 1 就不乘
                    double alpha = U.toDouble(s[3].trim());
                    if (alpha >= 0 && alpha <= 1) {
                        a = (int) (alpha * 255);
                    } else if (alpha > 1) {
                        a = (int) alpha;
                    }
                }
            }
        }
        if (r < 0 || r > 255) { r = 57; }
        if (g < 0 || g > 255) { g = 66; }
        if (b < 0 || b > 255) { b = 108; }
        if (a < 0 || a > 255) { a = 255; }

        // ========== 上面处理参数的默认值 ==========

        BufferedImage image = new BufferedImage(widthCount, heightCount, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.createGraphics();
        // 图像背景填充为灰色
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.fillRect(0, 0, widthCount, heightCount);

        // 画一些干扰线
        int interferenceCount = loop * 10;
        int maxRandom = 256;
        for (int i = 0; i < interferenceCount; i++) {
            graphics.setColor(new Color(RANDOM.nextInt(maxRandom), RANDOM.nextInt(maxRandom), RANDOM.nextInt(maxRandom)));
            graphics.drawLine(RANDOM.nextInt(widthCount), RANDOM.nextInt(heightCount),
                    RANDOM.nextInt(widthCount), RANDOM.nextInt(heightCount));
        }
        graphics.setColor(new Color(r, g, b, a));

        int x = (widthCount - 8) / (loop + 1);
        int y = heightCount - 5;
        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < loop; i++) {
            String value = U.toStr(str.charAt(RANDOM.nextInt(str.length())));
            // 字体大小
            graphics.setFont(new Font(FONTS[RANDOM.nextInt(FONTS.length)], Font.BOLD, heightCount - RANDOM.nextInt(8)));
            graphics.drawString(value, (i + 1) * x, y);
            sbd.append(value);
        }
        return new Code(sbd.toString(), image);
    }

    private static int toInt(String str) {
        if (str == null) {
            return 0;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Code {
        private String content;
        private BufferedImage image;
    }
}
