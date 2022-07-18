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
     * @param background 背景颜色, 不传则默认是 192,192,192,1
     * @param color 生成的图片上面验证码的颜色, 不传则默认是 57,66,108,1
     */
    public static Code generateCode(String count, String style, String width,
                                    String height, String background, String color) {
        // 默认是数字 + 英文
        String str = "w".equalsIgnoreCase(style) ? WORD : ("n".equalsIgnoreCase(style) ? NUMBER : WORD_NUMBER);
        int loop = Math.max(toInt(count), 4);
        int widthCount = Math.max(toInt(width), 100);
        int heightCount = Math.max(toInt(height), 30);

        // 字体颜色
        Color colorRgba = parse(color, 57, 66, 108);
        // 背景颜色
        Color backgroundRgba = parse(background, 192, 192, 192);

        // ========== 上面处理参数的默认值 ==========

        BufferedImage image = new BufferedImage(widthCount, heightCount, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.createGraphics();
        // 背景颜色
        graphics.setColor(backgroundRgba);
        graphics.fillRect(0, 0, widthCount, heightCount);

        int interferenceCount = loop * 10;
        int maxRandom = 256;
        for (int i = 0; i < interferenceCount; i++) {
            // 随机线
            graphics.setColor(new Color(RANDOM.nextInt(maxRandom), RANDOM.nextInt(maxRandom), RANDOM.nextInt(maxRandom)));
            graphics.drawLine(RANDOM.nextInt(widthCount), RANDOM.nextInt(heightCount),
                    RANDOM.nextInt(widthCount), RANDOM.nextInt(heightCount));
        }
        // 内容颜色
        graphics.setColor(colorRgba);

        int x = (widthCount - 8) / (loop + 1);
        int y = heightCount - 5;
        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < loop; i++) {
            // 随机字体
            graphics.setFont(new Font(FONTS[RANDOM.nextInt(FONTS.length)], Font.BOLD, heightCount - RANDOM.nextInt(8)));
            String value = toStr(str.charAt(RANDOM.nextInt(str.length())));
            graphics.drawString(value, (i + 1) * x, y);
            sbd.append(value);
        }
        return new Code(sbd.toString(), image);
    }

    /** @see java.awt.Color#Color(float, float, float, float) */
    private static Color parse(String rgba, int defaultRed, int defaultGreen, int defaultBlue) {
        int red = -1, green = -1, blue = -1, alpha = -1;

        if (isNotBlank(rgba)) {
            String[] s = rgba.split(",");
            red = toInt(s[0].trim());
            int len = s.length;
            if (len > 1) {
                green = toInt(s[1].trim());
                if (len > 2) {
                    blue = toInt(s[2].trim());
                }
                if (len > 3) {
                    // alpha 在 0 ~ 1 之间, 就乘以 255, 大于 1 就不乘
                    double a = toDouble(s[3].trim());
                    if (a >= 0 && a <= 1) {
                        alpha = (int) (a * 255 + 0.5);
                    } else if (a > 1) {
                        alpha = (int) a;
                    }
                }
            }
        }

        if (red < 0 || red > 255) { red = defaultRed; }
        if (green < 0 || green > 255) { green = defaultGreen; }
        if (blue < 0 || blue > 255) { blue = defaultBlue; }
        if (alpha < 0 || alpha > 255) { alpha = 255; }

        return new Color(red, green, blue, alpha);
    }

    private static int toInt(String str) {
        if (str == null) {
            return -1;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    private static double toDouble(String str) {
        if (str == null) {
            return -1;
        }
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    private static String toStr(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private static boolean isNotBlank(String str) {
        return str != null && !str.isEmpty();
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
