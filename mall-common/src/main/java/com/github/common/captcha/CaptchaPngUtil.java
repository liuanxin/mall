package com.github.common.captcha;

import com.github.common.util.Obj;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

/**
 * 中文点选验证码工具(png 格式).
 * 布局: 上方提示区 + 下方点击区.
 */
public class CaptchaPngUtil {

    /**
     * @param dark 可选, 为 1/true/on(忽略大小写) 时使用深色主题; 否则浅色
     */
    public static Captcha.Build buildClickCaptcha(String width, String height, String dark) {
        int imageWidth = Captcha.resolveWidth(width);
        int imageHeight = Captcha.resolveHeight(height);
        boolean darkBg = Obj.toBool(dark);

        int targetCount = Captcha.randomTargetCount();
        int noiseCount = Captcha.randomNoiseCount(targetCount);
        List<String> targetChars = Captcha.pickRenderableTargets(targetCount);
        List<String> noiseChars = Captcha.pickRenderableNoise(targetChars, noiseCount);

        int promptBottom = Captcha.promptBottom(imageHeight);
        int clickHeight = imageHeight - promptBottom;
        int clickFontSize = Math.max(15, Math.min(20, clickHeight / 3));
        List<Captcha.Point> points = Captcha.randomPoints(imageWidth, clickHeight, clickFontSize, targetChars.size() + noiseChars.size());

        // light/dark 模式下底色略随机, 提示字/分割线跟着派生
        Color bg = darkBg ? new Color(20 + Obj.RANDOM.nextInt(25), 20 + Obj.RANDOM.nextInt(25), 20 + Obj.RANDOM.nextInt(25))
                : new Color(235 + Obj.RANDOM.nextInt(20), 235 + Obj.RANDOM.nextInt(20), 240 + Obj.RANDOM.nextInt(15));
        Color primary = darkBg ? new Color(230, 230, 235) : new Color(40, 50, 65);
        Color secondary = darkBg ? new Color(160, 160, 170) : new Color(100, 110, 125);
        Color divider = darkBg ? new Color(60, 60, 70) : new Color(190, 200, 210);

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        List<Captcha.Glyph> glyphList = new ArrayList<>();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(bg);
            g.fillRect(0, 0, imageWidth, imageHeight);

            g.setColor(secondary);
            g.setFont(Captcha.pickFont(10, false));
            g.drawString("请在下方依次点击", 6, Math.max(14, Captcha.PROMPT_AREA_HEIGHT * 20 / 32));
            g.setColor(primary);
            int promptFont = Math.max(14, Math.min(18, promptBottom - 8));
            g.setFont(Captcha.pickFont(promptFont, false).deriveFont(Font.BOLD));
            FontMetrics pfm = g.getFontMetrics();
            int slot = promptFont + 4;
            int startX = imageWidth - 6 - targetChars.size() * slot;
            for (int i = 0; i < targetChars.size(); i++) {
                String ch = targetChars.get(i);
                int cx = startX + i * slot + slot / 2;
                int cy = promptBottom / 2;
                AffineTransform old = g.getTransform();
                g.rotate(Math.toRadians(Obj.RANDOM.nextInt(31) - 15), cx, cy);
                g.drawString(ch, cx - pfm.stringWidth(ch) / 2, cy + (pfm.getAscent() - pfm.getDescent()) / 2);
                g.setTransform(old);
            }
            g.setColor(divider);
            g.drawLine(0, promptBottom, imageWidth, promptBottom);

            int idx = 0;
            for (int i = 0; i < targetChars.size(); i++) {
                Captcha.Point p = points.get(idx++);
                glyphList.add(drawGlyph(g, targetChars.get(i), p.x(), p.y() + promptBottom, true, i,
                        clickFontSize, imageWidth, promptBottom, imageHeight, darkBg));
            }
            for (String noise : noiseChars) {
                Captcha.Point p = points.get(idx++);
                glyphList.add(drawGlyph(g, noise, p.x(), p.y() + promptBottom, false, -1,
                        clickFontSize, imageWidth, promptBottom, imageHeight, darkBg));
            }
        } finally {
            g.dispose();
        }
        return new Captcha.Build(toDataUri(image), imageWidth, imageHeight,
                new Captcha.Challenge(targetChars, glyphList, imageWidth, imageHeight, promptBottom));
    }

    private static Captcha.Glyph drawGlyph(
            Graphics2D g, String value, int cx, int cy, boolean target, int targetOrder,
            int baseFontSize, int imageWidth, int clickTop, int imageHeight, boolean darkBg
    ) {
        int fontSize = Math.max(13, Math.min(28, baseFontSize + Obj.RANDOM.nextInt(9) - 3));
        Font font = Captcha.pickFont(fontSize, true);
        g.setFont(font);
        float alpha = 0.72f + Obj.RANDOM.nextFloat() * 0.28f;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(randomGlyphColor(darkBg));
        FontMetrics fm = g.getFontMetrics();
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.rotate(Math.toRadians(Obj.RANDOM.nextInt(49) - 24));
        g.drawString(value, -fm.stringWidth(value) / 2, (fm.getAscent() - fm.getDescent()) / 2);
        g.setTransform(old);
        g.setComposite(AlphaComposite.SrcOver);
        int radius = Math.max(10, fontSize / 2 + 4);
        return new Captcha.Glyph(value,
                Math.min(imageWidth - 4, Math.max(4, cx)),
                Math.min(imageHeight - 4, Math.max(clickTop + 4, cy)),
                radius, target, targetOrder);
    }

    /** 每个字独立随机色; dark 为 true 时配黑底用浅色字 */
    private static Color randomGlyphColor(boolean darkBg) {
        float h = Obj.RANDOM.nextFloat();
        if (darkBg) {
            return Color.getHSBColor(h, 0.05f + Obj.RANDOM.nextFloat() * 0.5f, 0.72f + Obj.RANDOM.nextFloat() * 0.28f);
        }
        return Color.getHSBColor(h, 0.22f + Obj.RANDOM.nextFloat() * 0.6f, 0.18f + Obj.RANDOM.nextFloat() * 0.44f);
    }

    private static String toDataUri(BufferedImage image) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            boolean ok = writePngMaxCompress(image, stream);
            if (!ok) {
                stream.reset();
                ok = ImageIO.write(image, "png", stream);
            }
            if (ok && stream.size() > 0) {
                String pngBase64 = new String(Base64.getEncoder().encode(stream.toByteArray()), StandardCharsets.UTF_8);
                return "data:image/png;base64," + pngBase64;
            }
        } catch (Exception ignore) {
        }
        return "";
    }

    /** PNG zlib 显式最高压缩, 比默认 ImageIO.write 往往更小 */
    private static boolean writePngMaxCompress(BufferedImage image, ByteArrayOutputStream stream) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (writers == null || !writers.hasNext()) {
            return false;
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] types = param.getCompressionTypes();
                if (types != null && types.length > 0) {
                    param.setCompressionType(types[0]);
                }
                param.setCompressionQuality(0.0f);
            }
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(stream)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            writer.dispose();
        }
    }


}
