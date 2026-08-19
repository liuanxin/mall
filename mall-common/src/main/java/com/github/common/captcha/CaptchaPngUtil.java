package com.github.common.captcha;

import com.github.common.util.Arr;
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
import java.util.*;
import java.util.List;

/**
 * 中文点选验证码工具(png 格式).
 * 布局: 上方提示区 + 下方点击区.
 */
@SuppressWarnings("DuplicatedCode")
public final class CaptchaPngUtil {

    /** GET /captcha 未传 width 时的默认像素 */
    public static final int DEFAULT_CAPTCHA_WIDTH = 224;
    /** GET /captcha 未传 height 时的默认像素(含提示区) */
    public static final int DEFAULT_CAPTCHA_HEIGHT = 88;
    /** 提示区高度(像素), 与前端约定: 仅允许点击此线以下 */
    public static final int PROMPT_AREA_HEIGHT = 32;
    private static final int CAPTCHA_WIDTH_MIN = 200;
    private static final int CAPTCHA_WIDTH_MAX = 400;
    private static final int CAPTCHA_HEIGHT_MIN = 72;
    private static final int CAPTCHA_HEIGHT_MAX = 140;
    public static final int TARGET_COUNT_MIN = CaptchaChars.TARGET_COUNT_MIN;
    public static final int TARGET_COUNT_MAX = CaptchaChars.TARGET_COUNT_MAX;
    private static final int NOISE_COUNT_MIN = 2;
    private static final int GLYPH_TOTAL_MAX = 7;
    private static final int DEFAULT_CLICK_TOLERANCE_PX = 12;

    /** 启动时探测可显示中文的字体族 */
    private static final String GLYPH_FONT_FAMILY = resolveGlyphFontFamily();
    private static final String RENDERABLE_NOISE_POOL;
    private static final List<String> RENDERABLE_GROUPS;
    private static final List<String> RENDERABLE_NUMBER_TRIPLETS;
    static {
        Font font = glyphFont(18);
        RENDERABLE_NOISE_POOL = CaptchaChars.filterNoisePool(font);
        RENDERABLE_GROUPS = CaptchaChars.filterGroups(font);
        RENDERABLE_NUMBER_TRIPLETS = CaptchaChars.filterNumberTriplets(font);
    }

    private CaptchaPngUtil() {
    }

    /**
     * @param dark 可选, 为 1/true/on(忽略大小写) 时使用深色主题; 否则浅色
     */
    public static CaptchaRecord.Build buildClickCaptcha(String width, String height, String dark) {
        int imageWidth = resolveSize(width, DEFAULT_CAPTCHA_WIDTH, CAPTCHA_WIDTH_MIN, CAPTCHA_WIDTH_MAX);
        int imageHeight = resolveSize(height, DEFAULT_CAPTCHA_HEIGHT, CAPTCHA_HEIGHT_MIN, CAPTCHA_HEIGHT_MAX);
        boolean darkBg = Obj.toBool(dark);

        int targetCount = TARGET_COUNT_MIN + Obj.RANDOM.nextInt(TARGET_COUNT_MAX - TARGET_COUNT_MIN + 1);
        int noiseCount = NOISE_COUNT_MIN + Obj.RANDOM.nextInt(GLYPH_TOTAL_MAX - targetCount - NOISE_COUNT_MIN + 1);
        List<String> targetChars = CaptchaChars.pickTargets(targetCount, RENDERABLE_GROUPS, RENDERABLE_NUMBER_TRIPLETS);
        List<String> noiseChars = CaptchaChars.pickNoise(targetChars, noiseCount, RENDERABLE_NOISE_POOL);

        // 提示区高度固定, 与前端 CAPTCHA_PROMPT_AREA_HEIGHT 保持一致
        int promptBottom = Math.min(PROMPT_AREA_HEIGHT, Math.max(24, imageHeight - 40));
        int clickHeight = imageHeight - promptBottom;
        int clickFontSize = Math.max(15, Math.min(20, clickHeight / 3));
        List<CaptchaRecord.Point> points = randomPoints(imageWidth, clickHeight, clickFontSize, targetChars.size() + noiseChars.size());

        // light/dark 模式下底色略随机, 提示字/分割线跟着派生
        Color bg = darkBg ? new Color(20 + Obj.RANDOM.nextInt(25), 20 + Obj.RANDOM.nextInt(25), 20 + Obj.RANDOM.nextInt(25))
                : new Color(235 + Obj.RANDOM.nextInt(20), 235 + Obj.RANDOM.nextInt(20), 240 + Obj.RANDOM.nextInt(15));
        Color primary = darkBg ? new Color(230, 230, 235) : new Color(40, 50, 65);
        Color secondary = darkBg ? new Color(160, 160, 170) : new Color(100, 110, 125);
        Color divider = darkBg ? new Color(60, 60, 70) : new Color(190, 200, 210);

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        List<CaptchaRecord.Glyph> glyphList = new ArrayList<>();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(bg);
            g.fillRect(0, 0, imageWidth, imageHeight);

            g.setColor(secondary);
            g.setFont(glyphFont(13));
            g.drawString("请依次点击", 10, Math.max(16, PROMPT_AREA_HEIGHT * 21 / 32));
            g.setColor(primary);
            int promptFont = Math.max(14, Math.min(18, promptBottom - 8));
            g.setFont(glyphFont(promptFont));
            FontMetrics pfm = g.getFontMetrics();
            int slot = promptFont + 6;
            int startX = imageWidth - 8 - targetChars.size() * slot;
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
                CaptchaRecord.Point p = points.get(idx++);
                glyphList.add(drawGlyph(g, targetChars.get(i), p.x(), p.y() + promptBottom, true, i,
                        clickFontSize, imageWidth, promptBottom, imageHeight, darkBg));
            }
            for (String noise : noiseChars) {
                CaptchaRecord.Point p = points.get(idx++);
                glyphList.add(drawGlyph(g, noise, p.x(), p.y() + promptBottom, false, -1,
                        clickFontSize, imageWidth, promptBottom, imageHeight, darkBg));
            }
        } finally {
            g.dispose();
        }
        return new CaptchaRecord.Build(toDataUri(image), imageWidth, imageHeight,
                new CaptchaRecord.Challenge(targetChars, glyphList, imageWidth, imageHeight, promptBottom));
    }

    private static CaptchaRecord.Glyph drawGlyph(
            Graphics2D g, String value, int cx, int cy, boolean target, int targetOrder,
            int baseFontSize, int imageWidth, int clickTop, int imageHeight, boolean darkBg
    ) {
        int fontSize = Math.max(13, Math.min(28, baseFontSize + Obj.RANDOM.nextInt(9) - 3));
        Font font = glyphFont(fontSize);
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
        return new CaptchaRecord.Glyph(value,
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

    /**
     * 解析 URL 中的宽高: 未传或非法则用默认值, 否则夹在 min~max 防止过大图拖垮服务.
     */
    private static int resolveSize(String param, int defaultPx, int minPx, int maxPx) {
        if (Obj.isBlank(param)) {
            return defaultPx;
        }
        int v = Obj.toInt(param);
        if (v <= 0) {
            return defaultPx;
        }
        return Math.min(maxPx, Math.max(minPx, v));
    }


    /**
     * 每个字独占一段水平区间并在区间内抖动, 避免纯随机失败后降级成「忽略间距」导致叠字.
     */
    private static List<CaptchaRecord.Point> randomPoints(int width, int height, int fontSize, int count) {
        int padX = Math.min(fontSize + 14, Math.max(10, width / 5));
        int padY = Math.min(fontSize / 2 + 6, Math.max(8, height / 3));
        int innerW = Math.max(1, width - padX * 2);
        int innerH = Math.max(1, height - padY * 2);
        List<CaptchaRecord.Point> list = new ArrayList<>(count);
        List<Integer> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, Obj.RANDOM);
        double cellW = innerW / (double) count;
        for (int i = 0; i < count; i++) {
            int slot = slots.get(i);
            int x = (int) Math.round(padX + (slot + 0.5) * cellW);
            int y = padY + Obj.RANDOM.nextInt(innerH);
            list.add(new CaptchaRecord.Point(Math.min(width - padX - 1, Math.max(padX, x)), y));
        }
        return list;
    }

    private static Font glyphFont(int size) {
        if (Obj.isNotBlank(GLYPH_FONT_FAMILY)) {
            return new Font(GLYPH_FONT_FAMILY, Font.PLAIN, size);
        }
        return new Font(Font.DIALOG, Font.PLAIN, size);
    }

    private static String resolveGlyphFontFamily() {
        String probe = "验证码春夏秋";
        String[] candidates = {
                "Noto Sans CJK SC", "Source Han Sans SC", "Microsoft YaHei",
                "WenQuanYi Zen Hei", "WenQuanYi Micro Hei", "PingFang SC",
                "Hiragino Sans GB", "SimHei", "SimSun",
                "Droid Sans Fallback"
        };
        try {
            Set<String> families = new HashSet<>();
            for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                families.add(f.toLowerCase());
            }
            for (String c : candidates) {
                if (families.contains(c.toLowerCase())
                        && new Font(c, Font.PLAIN, 18).canDisplayUpTo(probe) == -1) {
                    return c;
                }
            }
        } catch (Exception ignore) {
        }
        return "";
    }


    public static boolean verifyClick(CaptchaRecord.Challenge challenge, List<CaptchaRecord.PointInput> points, Integer tolerancePx) {
        if (challenge == null || Arr.isEmpty(points) || Arr.isEmpty(challenge.targetChars())) {
            return false;
        }
        int targetCount = challenge.targetChars().size();
        if (points.size() != targetCount) {
            return false;
        }
        int tolerance = Obj.toInt(tolerancePx, DEFAULT_CLICK_TOLERANCE_PX);
        if (tolerance <= 0) {
            tolerance = DEFAULT_CLICK_TOLERANCE_PX;
        }
        int clickTop = challenge.clickAreaTop() > 0 ? challenge.clickAreaTop() : PROMPT_AREA_HEIGHT;
        for (int i = 0; i < targetCount; i++) {
            CaptchaRecord.PointInput pointInput = points.get(i);
            if (pointInput == null || pointInput.x() < 0 || pointInput.x() > 1
                    || pointInput.y() < 0 || pointInput.y() > 1) {
                return false;
            }
            int px = (int) Math.round(pointInput.x() * challenge.width());
            int py = (int) Math.round(pointInput.y() * challenge.height());
            if (py < clickTop) {
                return false;
            }
            CaptchaRecord.Glyph targetGlyph = null;
            for (CaptchaRecord.Glyph glyph : challenge.glyphList()) {
                if (glyph.target() && glyph.targetOrder() == i) {
                    targetGlyph = glyph;
                    break;
                }
            }
            if (targetGlyph == null) {
                return false;
            }
            int radius = Math.max(targetGlyph.radius() + Math.min(4, tolerance / 3), tolerance);
            int dx = targetGlyph.x() - px;
            int dy = targetGlyph.y() - py;
            if (dx * dx + dy * dy > radius * radius) {
                return false;
            }
        }
        return true;
    }
}
