package com.github.common.util;

import com.github.common.encrypt.Encrypt;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** 中文点选验证码工具 */
@SuppressWarnings("DuplicatedCode")
public final class CaptchaUtil {

    /** GET /captcha 未传 width 时的默认像素, 与前端约定一致 */
    public static final int DEFAULT_CAPTCHA_WIDTH = 224;
    /** GET /captcha 未传 height 时的默认像素, 与前端约定一致(扁横幅, 约 224:48) */
    public static final int DEFAULT_CAPTCHA_HEIGHT = 48;
    private static final int CAPTCHA_WIDTH_MIN = 200;
    private static final int CAPTCHA_WIDTH_MAX = 400;
    private static final int CAPTCHA_HEIGHT_MIN = 44;
    private static final int CAPTCHA_HEIGHT_MAX = 108;

    private static final String COMMON_CHAR_POOL;
    static {
        String s = """
                零一二三四五六七八九十百千万亿壹贰叁肆伍陆柒捌玖个拾佰仟萬億兆大小
                甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥春夏秋冬东西南北中柴米油盐酱醋茶猫鼠牛虎兔龙蛇马羊猴鸡狗猪
                我人有的和主不为这工要在地上是经以发了同他来到说们你出也时年得就那下生会自着去之过学对可里后
                么心多天而能好都然没于起成事只作当看文开手用方又如前见头面公已从动两俩仨长知样现分定进其泊
                北京天津河山内古海苏浙徽福建湖广重川贵云陕甘青夏香港澳石家庄唐岛邯郸邢定承德沧坊水太原同阳泉
                运汾吕梁呼和浩包头乌通贝巴兰阿拉善沈连丹锦营盘岭葫芦春源化松边哈滨齐鹤双鸭伊佳木牡地徐常淮
                盐扬泰宿迁杭波嘉绍金华舟丽合芜蚌埠马铜陵亳池宣厦莆田漳岩昌景乡余宜饶济枣烟潍照莱沂聊菏郑
                封洛焦作许商丘店孝感冈咸随施苗沙洲益娄底韶关深圳珠汕湛茂名惠梅尾远莞群揭
                浮柳桂梧钦玉宾左亚枝花泸绵元遂乐充眉达雅资坝仁依节曲普楚尼版纳理宏颇迪萨则芝峪吴玛音什犁
                苹果香蕉橙葡萄瓜桃梨枣柚柠檬樱芒果榴莲山楂甘蔗番茄土豆萝卜菠芹菜生豆角辣椒洋葱蒜姜
                """;
        Set<String> set = new LinkedHashSet<>();
        for (char c : s.toCharArray()) {
            String v = String.valueOf(c).trim();
            if (!v.isEmpty()) {
                set.add(v);
            }
        }
        COMMON_CHAR_POOL = String.join("", set);
    }

    private static final int DEFAULT_CLICK_TOLERANCE_PX = 24;

    /**
     * @param dark 可选, 为 1/true/on(忽略大小写) 时使用纯黑底并配浅色字; 否则为浅灰底.
     */
    public static CaptchaBuild buildClickCaptcha(String width, String height, String dark) {
        int imageWidth = resolveCaptchaWidthOrHeight(width, DEFAULT_CAPTCHA_WIDTH, CAPTCHA_WIDTH_MIN, CAPTCHA_WIDTH_MAX);
        int imageHeight = resolveCaptchaWidthOrHeight(height, DEFAULT_CAPTCHA_HEIGHT, CAPTCHA_HEIGHT_MIN, CAPTCHA_HEIGHT_MAX);
        boolean darkBg = Obj.toBool(dark);

        int targetCount = 3;
        // 必点 3 字 + 少量干扰, 合计约 4 ~ 6 个字
        int noiseCount = Obj.RANDOM.nextInt(3) + 1;
        List<String> targetChars = randomUniqueChars(targetCount, null);
        List<String> noiseChars = randomUniqueChars(noiseCount, targetChars);

        int glyphCount = targetCount + noiseCount;
        int baseFontSize = Math.min(pickGlyphFontSize(glyphCount), Math.max(14, imageHeight * 2 / 5));
        List<CaptchaPoint> points = randomPoints(imageWidth, imageHeight, glyphCount, baseFontSize);
        return buildSvgCaptcha(targetChars, noiseChars, points, baseFontSize, imageWidth, imageHeight, darkBg);
    }

    /**
     * 解析 URL 中的宽高: 未传或非法则用默认值, 否则夹在 min~max 防止过大图拖垮服务.
     */
    private static int resolveCaptchaWidthOrHeight(String param, int defaultPx, int minPx, int maxPx) {
        if (Obj.isBlank(param)) {
            return defaultPx;
        }
        int v = Obj.toInt(param);
        if (v <= 0) {
            return defaultPx;
        }
        return Math.min(maxPx, Math.max(minPx, v));
    }

    private static CaptchaBuild buildSvgCaptcha(
            List<String> targetChars,
            List<String> noiseChars,
            List<CaptchaPoint> points,
            int baseFontSize,
            int imageWidth,
            int imageHeight,
            boolean darkBg
    ) {
        List<CaptchaGlyph> glyphList = new ArrayList<>();
        List<GlyphPaint> paints = new ArrayList<>();
        int pointIndex = 0;
        for (int i = 0; i < targetChars.size(); i++) {
            CaptchaPoint point = points.get(pointIndex++);
            GlyphPaint paint = randomGlyphPaint(targetChars.get(i), point, true, i, baseFontSize, imageWidth, imageHeight, darkBg);
            glyphList.add(paint.glyph());
            paints.add(paint);
        }
        for (String noiseChar : noiseChars) {
            CaptchaPoint point = points.get(pointIndex++);
            GlyphPaint paint = randomGlyphPaint(noiseChar, point, false, -1, baseFontSize, imageWidth, imageHeight, darkBg);
            glyphList.add(paint.glyph());
            paints.add(paint);
        }
        String svgText = buildSvgText(imageWidth, imageHeight, darkBg, targetChars.size() + noiseChars.size(), paints);
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("生成的 svg({})", svgText);
        }
        String dataUri = "data:image/svg+xml;base64,%s".formatted(Encrypt.base64StringEncode(svgText.getBytes(StandardCharsets.UTF_8)));
        String promptText = "请依次点击: %s %s %s".formatted(targetChars.get(0), targetChars.get(1), targetChars.get(2));
        CaptchaChallenge challenge = new CaptchaChallenge(targetChars, glyphList, imageWidth, imageHeight);
        return new CaptchaBuild(dataUri, promptText, imageWidth, imageHeight, challenge);
    }

    private static GlyphPaint randomGlyphPaint(
            String value,
            CaptchaPoint point,
            boolean target,
            int targetOrder,
            int baseFontSize,
            int imageWidth,
            int imageHeight,
            boolean darkBg
    ) {
        String fillColor = randomGlyphColorHex(darkBg);
        int glyphFontSize = randomGlyphFontSize(baseFontSize, imageHeight);
        int rotateDeg = randomGlyphRotateDeg();
        double opacity = randomGlyphOpacity();
        float strokeWidth = randomGlyphStrokeWidth();
        String strokeColor = strokeWidth > 0 ? randomGlyphStrokeHex(fillColor, darkBg) : null;
        return layoutGlyphSvg(value, point, target, targetOrder, glyphFontSize, imageWidth,
                imageHeight, fillColor, rotateDeg, opacity, strokeWidth, strokeColor);
    }

    private static String buildSvgText(int width, int height, boolean darkBg, int glyphCount, List<GlyphPaint> paints) {
        StringBuilder svg = new StringBuilder(2048);
        svg.append("""
                <svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d"><rect width="%d" height="%d" fill="%s"/>
                """.trim().formatted(width, height, width, height, width, height, getBackgroundColorHex(darkBg)));
        appendNoiseLinesSvg(svg, width, height, glyphCount, false, darkBg);
        for (GlyphPaint paint : paints) {
            svg.append("""
                    <text x="%d" y="%d" fill="%s" fill-opacity="%s" font-size="%d" transform="rotate(%d %d %d)"%s>%s</text>
                    """.trim().formatted(
                    paint.drawX(),
                    paint.drawY(),
                    paint.fillColor(),
                    formatOpacity(paint.opacity()),
                    paint.fontSize(),
                    paint.rotateDeg(),
                    paint.glyph().x(),
                    paint.glyph().y(),
                    strokeAttr(paint),
                    escapeXml(paint.value())
            ));
        }
        appendNoiseLinesSvg(svg, width, height, glyphCount, true, darkBg);
        svg.append("</svg>");
        return svg.toString();
    }

    private static void appendNoiseLinesSvg(
            StringBuilder svg, int width, int height, int glyphCount, boolean overGlyphs, boolean darkBg
    ) {
        int lineCount = overGlyphs ? 3 : 6 + Math.max(0, 4 - glyphCount);
        float strokeW = overGlyphs ? 0.75f : 1.0f;
        String[] palette = resolveNoisePalette(overGlyphs, darkBg);
        for (int i = 0; i < lineCount; i++) {
            String stroke = palette[i % palette.length];
            int x1 = Obj.RANDOM.nextInt(Math.max(width, 1));
            int y1 = Obj.RANDOM.nextInt(Math.max(height, 1));
            int x2 = Obj.RANDOM.nextInt(Math.max(width, 1));
            int y2 = Obj.RANDOM.nextInt(Math.max(height, 1));
            svg.append("""
                    <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="%s"%s/>
                    """.trim().formatted(x1, y1, x2, y2, stroke, (strokeW == 1.0f ? "" : " stroke-width=\"%s\"".formatted(strokeW))));
        }
    }

    private static String strokeAttr(GlyphPaint paint) {
        if (paint.strokeWidth() <= 0 || Obj.isBlank(paint.strokeColor())) {
            return "";
        }
        return """
                %sstroke="%s" stroke-opacity="0.55" stroke-width="%s"
                """.trim().formatted(" ", paint.strokeColor(), paint.strokeWidth());
    }

    /** 总字数越少字号略小, 避免在矮图里显得过大 */
    private static int pickGlyphFontSize(int glyphCount) {
        return switch (glyphCount) {
            case 3 -> 15 + Obj.RANDOM.nextInt(4);
            case 4 -> 16 + Obj.RANDOM.nextInt(4);
            case 5 -> 17 + Obj.RANDOM.nextInt(4);
            default -> 18 + Obj.RANDOM.nextInt(5);
        };
    }

    /** 单字轻微字号浮动, 保证读感有变化但不至于明显挤压布局 */
    private static int randomGlyphFontSize(int baseFontSize, int imageHeight) {
        int v = baseFontSize + (Obj.RANDOM.nextInt(5) - 2);
        int min = 12;
        int max = Math.max(min, imageHeight / 2 + 1);
        return Math.max(min, Math.min(max, v));
    }

    /** 轻微随机旋转角度, 让字形更自然但不影响可读性 */
    private static int randomGlyphRotateDeg() {
        return Obj.RANDOM.nextInt(21) - 10;
    }

    /** 轻微透明度抖动, 让颜色层次更自然 */
    private static double randomGlyphOpacity() {
        return 0.78 + Obj.RANDOM.nextDouble() * 0.22;
    }

    /** 小概率加极细描边, 提升字形边界变化 */
    private static float randomGlyphStrokeWidth() {
        return Obj.RANDOM.nextInt(100) < 35 ? 0.45f + Obj.RANDOM.nextFloat() * 0.35f : 0F;
    }

    private static String randomGlyphStrokeHex(String fillColor, boolean darkBg) {
        return adjustHexColor(fillColor, darkBg ? -56 : 42);
    }

    private static String adjustHexColor(String hexColor, int delta) {
        if (Obj.isBlank(hexColor) || hexColor.length() != 7 || !hexColor.startsWith("#")) {
            return hexColor;
        }
        int r = parseHex(hexColor.substring(1, 3));
        int g = parseHex(hexColor.substring(3, 5));
        int b = parseHex(hexColor.substring(5, 7));
        r = clamp255(r + delta);
        g = clamp255(g + delta);
        b = clamp255(b + delta);
        return String.format(Locale.ROOT, "#%02x%02x%02x", r, g, b);
    }

    private static int parseHex(String s) {
        try {
            return Integer.parseInt(s, 16);
        } catch (Exception ignore) {
            return 0;
        }
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static List<String> randomUniqueChars(int count, List<String> exclude) {
        List<String> list = new ArrayList<>();
        int total = COMMON_CHAR_POOL.length();
        int guard = 0;
        while (list.size() < count && guard < total * 4) {
            guard++;
            String value = String.valueOf(COMMON_CHAR_POOL.charAt(Obj.RANDOM.nextInt(total)));
            if (list.contains(value)) {
                continue;
            }
            if (exclude != null && exclude.contains(value)) {
                continue;
            }
            list.add(value);
        }
        return list;
    }

    /**
     * 每个字独占一段水平区间并在区间内抖动, 避免纯随机失败后降级成「忽略间距」导致叠字.
     */
    private static List<CaptchaPoint> randomPoints(int width, int height, int size, int fontSize) {
        int padX = Math.min(fontSize + 18, Math.max(12, width / 4));
        int padY = Math.min(fontSize + 10, Math.max(10, height / 2 - 2));
        if (width <= padX * 2 + 4) {
            padX = Math.max(6, width / 6);
        }
        if (height <= padY * 2 + 4) {
            padY = Math.max(6, height / 5);
        }
        int innerW = width - padX * 2;
        int innerH = height - padY * 2;
        List<CaptchaPoint> pointList = new ArrayList<>(size);
        if (innerW < 1 || innerH < 1 || size == 0) {
            return pointList;
        }
        double halfSpanX = fontSize * 0.55;
        double cellW = innerW / (double) size;
        double maxJitterX = cellW / 2.0 - halfSpanX - 4;
        if (maxJitterX < 0) {
            maxJitterX = 0;
        }
        List<Integer> slots = new ArrayList<>(size);
        for (int s = 0; s < size; s++) {
            slots.add(s);
        }
        Collections.shuffle(slots);
        for (int gi = 0; gi < size; gi++) {
            int slot = slots.get(gi);
            double baseX = padX + (slot + 0.5) * cellW;
            double jitterX = maxJitterX <= 0 ? 0 : (Obj.RANDOM.nextDouble() * 2 - 1) * maxJitterX;
            int x = (int) Math.round(baseX + jitterX);
            x = Math.min(width - padX - 1, Math.max(padX, x));
            int y = padY + Obj.RANDOM.nextInt(innerH);
            pointList.add(new CaptchaPoint(x, y));
        }
        return pointList;
    }

    private static String getBackgroundColorHex(boolean darkBg) {
        // return darkBg ? "#27272a" : "#fefefe";
        return darkBg ? "#141414" : "#f6f7fa";
    }

    /** 浅色底上的干扰线 */
    private static final String[] NOISE_LINE_RGB_BG_LIGHT = new String[] {
            "#c6cad2", "#b4bac4", "#d2d6dc"
    };
    private static final String[] NOISE_LINE_RGB_OVER_LIGHT = new String[] {
            "#d0d4da", "#b8bec8"
    };
    /** 纯黑底上的干扰线(略亮于黑) */
    private static final String[] NOISE_LINE_RGB_BG_DARK = new String[] {
            "#3c424c", "#505860", "#2c3038"
    };
    private static final String[] NOISE_LINE_RGB_OVER_DARK = new String[] {
            "#484e58", "#5c6470"
    };

    /** 每个字独立随机色; dark 为 true 时配黑底用浅色字 */
    private static String randomGlyphColorHex(boolean darkBg) {
        double h = Obj.RANDOM.nextDouble();
        double s;
        double v;
        if (darkBg) {
            s = 0.12 + Obj.RANDOM.nextDouble() * 0.42;
            v = 0.72 + Obj.RANDOM.nextDouble() * 0.26;
        } else {
            s = 0.32 + Obj.RANDOM.nextDouble() * 0.48;
            v = 0.26 + Obj.RANDOM.nextDouble() * 0.38;
        }
        return hsvToHex(h, s, v);
    }

    private static String hsvToHex(double h, double s, double v) {
        double hh = (h % 1.0 + 1.0) % 1.0;
        double c = v * s;
        double x = c * (1 - Math.abs((hh * 6) % 2 - 1));
        double m = v - c;
        double r1;
        double g1;
        double b1;
        if (hh < 1.0 / 6) {
            r1 = c;
            g1 = x;
            b1 = 0;
        } else if (hh < 2.0 / 6) {
            r1 = x;
            g1 = c;
            b1 = 0;
        } else if (hh < 3.0 / 6) {
            r1 = 0;
            g1 = c;
            b1 = x;
        } else if (hh < 4.0 / 6) {
            r1 = 0;
            g1 = x;
            b1 = c;
        } else if (hh < 5.0 / 6) {
            r1 = x;
            g1 = 0;
            b1 = c;
        } else {
            r1 = c;
            g1 = 0;
            b1 = x;
        }
        int r = clamp255((int) Math.round((r1 + m) * 255));
        int g = clamp255((int) Math.round((g1 + m) * 255));
        int b = clamp255((int) Math.round((b1 + m) * 255));
        return String.format(Locale.ROOT, "#%02x%02x%02x", r, g, b);
    }

    private static String[] resolveNoisePalette(boolean overGlyphs, boolean darkBg) {
        if (darkBg) {
            return overGlyphs ? NOISE_LINE_RGB_OVER_DARK : NOISE_LINE_RGB_BG_DARK;
        }
        return overGlyphs ? NOISE_LINE_RGB_OVER_LIGHT : NOISE_LINE_RGB_BG_LIGHT;
    }

    private static GlyphPaint layoutGlyphSvg(
            String value,
            CaptchaPoint point,
            boolean target,
            int targetOrder,
            int fontSize,
            int imgW,
            int imgH,
            String fillColor,
            int rotateDeg,
            double opacity,
            float strokeWidth,
            String strokeColor
    ) {
        int estimatedWidth = estimateGlyphWidth(value, fontSize);
        int ascent = Math.max(8, (int) Math.round(fontSize * 0.82));
        int descent = Math.max(2, fontSize - ascent);
        int drawX = point.x() - estimatedWidth / 2;
        int drawY = point.y() + (ascent - descent) / 2;
        final int bleed = 2;
        for (int k = 0; k < 4; k++) {
            int top = drawY - ascent;
            if (drawX < bleed) {
                drawX = bleed;
            }
            if (drawX + estimatedWidth > imgW - bleed) {
                drawX = Math.max(bleed, imgW - bleed - estimatedWidth);
            }
            if (top < bleed) {
                drawY += bleed - top;
            }
            int bottom = drawY + descent;
            if (bottom > imgH - bleed) {
                drawY -= bottom - (imgH - bleed);
            }
            top = drawY - ascent;
            if (top >= bleed && bottom <= imgH - bleed && drawX + estimatedWidth <= imgW - bleed) {
                break;
            }
        }
        int top = drawY - ascent;
        int bottom = drawY + descent;
        int cx = drawX + estimatedWidth / 2;
        int cy = (top + bottom) / 2;
        int radius = Math.max(ascent, estimatedWidth) / 2 + 6;
        CaptchaGlyph glyph = new CaptchaGlyph(value, cx, cy, radius, target, targetOrder);
        return new GlyphPaint(glyph, value, drawX, drawY, fillColor, fontSize, rotateDeg, opacity, strokeWidth, strokeColor);
    }

    private static int estimateGlyphWidth(String value, int fontSize) {
        if (Obj.isBlank(value)) {
            return Math.max(1, fontSize);
        }
        int width = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            width += c <= 0x7f ? (int) Math.round(fontSize * 0.55) : (int) Math.round(fontSize * 0.92);
        }
        return Math.max(1, width);
    }

    private static String formatOpacity(double opacity) {
        return String.format(Locale.ROOT, "%.3f", opacity);
    }

    private static String escapeXml(String src) {
        return src
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static boolean verifyClick(CaptchaChallenge challenge, List<CaptchaPointInput> points, Integer tolerancePx) {
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

        for (int i = 0; i < targetCount; i++) {
            CaptchaPointInput pointInput = points.get(i);
            if (pointInput == null) {
                return false;
            }
            if (pointInput.x() < 0 || pointInput.x() > 1 || pointInput.y() < 0 || pointInput.y() > 1) {
                return false;
            }
            int px = (int) Math.round(pointInput.x() * challenge.width());
            int py = (int) Math.round(pointInput.y() * challenge.height());
            CaptchaGlyph targetGlyph = findTargetGlyphByOrder(challenge.glyphList(), i);
            if (targetGlyph == null || !isHit(px, py, targetGlyph, tolerance)) {
                return false;
            }
        }
        return true;
    }

    private static CaptchaGlyph findTargetGlyphByOrder(List<CaptchaGlyph> glyphList, int order) {
        if (Arr.isEmpty(glyphList)) {
            return null;
        }
        for (CaptchaGlyph glyph : glyphList) {
            if (glyph.target() && glyph.targetOrder() == order) {
                return glyph;
            }
        }
        return null;
    }

    private static boolean isHit(int px, int py, CaptchaGlyph glyph, int tolerance) {
        int radius = Math.max(glyph.radius(), tolerance);
        int dx = glyph.x() - px;
        int dy = glyph.y() - py;
        return dx * dx + dy * dy <= radius * radius;
    }

    public record CaptchaPoint(int x, int y) { }

    private record GlyphPaint(
            CaptchaGlyph glyph,
            String value,
            int drawX,
            int drawY,
            String fillColor,
            int fontSize,
            int rotateDeg,
            double opacity,
            float strokeWidth,
            String strokeColor
    ) { }

    public record CaptchaGlyph(String value, int x, int y, int radius, boolean target, int targetOrder) { }

    public record CaptchaChallenge(List<String> targetChars, List<CaptchaGlyph> glyphList, int width, int height) { }

    public record CaptchaBuild(String base64, String text, int width, int height, CaptchaChallenge challenge) { }

    public record CaptchaPointInput(double x, double y) { }
}
