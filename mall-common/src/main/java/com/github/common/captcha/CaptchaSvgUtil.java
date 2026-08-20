package com.github.common.captcha;

import com.github.common.util.Obj;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 中文点选验证码(SVG text 拼接版, 体积小但字可被直接解析).
 * 布局: 上方提示区 + 下方点击区.
 */
@SuppressWarnings("DuplicatedCode")
public class CaptchaSvgUtil {

    /**
     * @param dark 可选, 为 1/true/on(忽略大小写) 时使用深色主题; 否则浅色
     */
    public static Captcha.Build buildClickCaptcha(String width, String height, String dark) {
        int imageWidth = Captcha.resolveWidth(width);
        int imageHeight = Captcha.resolveHeight(height);
        boolean darkBg = Obj.toBool(dark);

        int targetCount = Captcha.randomTargetCount();
        int noiseCount = Captcha.randomNoiseCount(targetCount);
        List<String> targetChars = Captcha.pickTargets(targetCount);
        List<String> noiseChars = Captcha.pickNoise(targetChars, noiseCount);

        int promptBottom = Captcha.promptBottom(imageHeight);
        int clickHeight = imageHeight - promptBottom;
        int clickFontSize = Math.max(15, Math.min(20, clickHeight / 3));
        List<Captcha.Point> points = Captcha.randomPoints(imageWidth, clickHeight, clickFontSize, targetChars.size() + noiseChars.size());

        // light/dark 模式下底色略随机(3 位 hex 更短)
        String bg = darkBg ? randomHex(0.06, 0.18, 0.02, 0.16) : randomHex(0.92, 0.99, 0.02, 0.14);
        String primary = darkBg ? "#eee" : "#345";
        String secondary = darkBg ? "#aaa" : "#678";
        String divider = darkBg ? "#444" : "#cde";

        List<Captcha.Glyph> glyphList = new ArrayList<>();
        StringBuilder body = new StringBuilder(1024);
        int idx = 0;
        for (int i = 0; i < targetChars.size(); i++) {
            Captcha.Point p = points.get(idx++);
            GlyphPaint paint = paintGlyph(targetChars.get(i), p.x(), p.y() + promptBottom, true, i,
                    clickFontSize, imageWidth, promptBottom, imageHeight, darkBg, bg);
            glyphList.add(paint.glyph());
            appendText(body, paint);
        }
        for (String noise : noiseChars) {
            Captcha.Point p = points.get(idx++);
            GlyphPaint paint = paintGlyph(noise, p.x(), p.y() + promptBottom, false, -1,
                    clickFontSize, imageWidth, promptBottom, imageHeight, darkBg, bg);
            glyphList.add(paint.glyph());
            appendText(body, paint);
        }

        // width/height 够用, 不再重复写 viewBox
        StringBuilder svg = new StringBuilder(1200);
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(imageWidth)
                .append("\" height=\"").append(imageHeight).append("\">");
        svg.append("<rect width=\"").append(imageWidth).append("\" height=\"").append(imageHeight)
                .append("\" fill=\"").append(bg).append("\"/>");
        svg.append("<text x=\"6\" y=\"").append(Math.max(14, Captcha.PROMPT_AREA_HEIGHT * 20 / 32))
                .append("\" font-size=\"10\" fill=\"").append(secondary).append("\">请在下方依次点击</text>");
        int promptFont = Math.max(14, Math.min(18, promptBottom - 8));
        int slot = promptFont + 4;
        int startX = imageWidth - 6 - targetChars.size() * slot;
        for (int i = 0; i < targetChars.size(); i++) {
            int cx = startX + i * slot + slot / 2;
            int cy = promptBottom / 2;
            int rotate = Obj.RANDOM.nextInt(31) - 15;
            int tx = cx - promptFont / 2;
            int ty = cy + promptFont / 3;
            svg.append("<text x=\"").append(tx).append("\" y=\"").append(ty)
                    .append("\" fill=\"").append(primary)
                    .append("\" font-size=\"").append(promptFont)
                    .append("\" font-weight=\"700\"");
            if (rotate != 0) {
                svg.append(" transform=\"rotate(").append(rotate).append(' ').append(cx).append(' ').append(cy).append(")\"");
            }
            svg.append('>').append(escapeXml(targetChars.get(i))).append("</text>");
        }
        // 分隔线: fill=none 必需, 否则默认填充会画脏
        svg.append("<path d=\"M0 ").append(promptBottom).append("h").append(imageWidth)
                .append("\" fill=\"none\" stroke=\"").append(divider).append("\"/>");
        svg.append(body).append("</svg>");

        String dataUri = "data:image/svg+xml," + encodeSvgDataUri(svg.toString());
        return new Captcha.Build(dataUri, imageWidth, imageHeight,
                new Captcha.Challenge(targetChars, glyphList, imageWidth, imageHeight, promptBottom));
    }

    private static GlyphPaint paintGlyph(
            String value, int cx, int cy, boolean target, int targetOrder,
            int baseFontSize, int imageWidth, int clickTop, int imageHeight, boolean darkBg, String bg
    ) {
        int fontSize = Math.max(13, Math.min(28, baseFontSize + Obj.RANDOM.nextInt(9) - 3));
        int rotate = Obj.RANDOM.nextInt(49) - 24;
        // 透明度预混合进 fill, 不再输出 fill-opacity
        double opacity = 0.72 + Obj.RANDOM.nextDouble() * 0.28;
        String raw = darkBg ? randomHex(0.72, 1.0, 0.05, 0.55) : randomHex(0.18, 0.62, 0.22, 0.82);
        String fill = blendHex(raw, bg, opacity);
        int radius = Math.max(10, fontSize / 2 + 4);
        Captcha.Glyph glyph = new Captcha.Glyph(value,
                Math.min(imageWidth - 4, Math.max(4, cx)),
                Math.min(imageHeight - 4, Math.max(clickTop + 4, cy)),
                radius, target, targetOrder);
        return new GlyphPaint(glyph, value, cx, cy, fill, fontSize, rotate);
    }

    private static void appendText(StringBuilder svg, GlyphPaint paint) {
        int tx = paint.cx() - paint.fontSize() / 2;
        int ty = paint.cy() + paint.fontSize() / 3;
        svg.append("<text x=\"").append(tx).append("\" y=\"").append(ty)
                .append("\" fill=\"").append(paint.fill())
                .append("\" font-size=\"").append(paint.fontSize()).append('"');
        if (paint.rotate() != 0) {
            svg.append(" transform=\"rotate(").append(paint.rotate())
                    .append(' ').append(paint.cx()).append(' ').append(paint.cy()).append(")\"");
        }
        svg.append('>').append(escapeXml(paint.value())).append("</text>");
    }

    /** 输出 #rgb 短格式 */
    private static String randomHex(double vMin, double vMax, double sMin, double sMax) {
        double h = Obj.RANDOM.nextDouble();
        double s = sMin + Obj.RANDOM.nextDouble() * (sMax - sMin);
        double v = vMin + Obj.RANDOM.nextDouble() * (vMax - vMin);
        double hh = h % 1.0;
        double c = v * s;
        double x = c * (1 - Math.abs((hh * 6) % 2 - 1));
        double m = v - c;
        double r1, g1, b1;
        //noinspection IfCanBeSwitch
        if (hh < 1.0 / 6) { r1 = c; g1 = x; b1 = 0; }
        else if (hh < 2.0 / 6) { r1 = x; g1 = c; b1 = 0; }
        else if (hh < 3.0 / 6) { r1 = 0; g1 = c; b1 = x; }
        else if (hh < 4.0 / 6) { r1 = 0; g1 = x; b1 = c; }
        else if (hh < 5.0 / 6) { r1 = x; g1 = 0; b1 = c; }
        else { r1 = c; g1 = 0; b1 = x; }
        int r = Math.max(0, Math.min(255, (int) Math.round((r1 + m) * 255)));
        int g = Math.max(0, Math.min(255, (int) Math.round((g1 + m) * 255)));
        int b = Math.max(0, Math.min(255, (int) Math.round((b1 + m) * 255)));
        return String.format(Locale.ROOT, "#%x%x%x",
                Math.round(r / 17.0), Math.round(g / 17.0), Math.round(b / 17.0));
    }

    /** 前景与背景按 alpha 预混合, 输出 #rgb */
    private static String blendHex(String fg, String bg, double alpha) {
        int[] f = parseRgb(fg);
        int[] b = parseRgb(bg);
        double a = Math.max(0, Math.min(1, alpha));
        int r = (int) Math.round(f[0] * a + b[0] * (1 - a));
        int g = (int) Math.round(f[1] * a + b[1] * (1 - a));
        int bl = (int) Math.round(f[2] * a + b[2] * (1 - a));
        return String.format(Locale.ROOT, "#%x%x%x",
                Math.round(r / 17.0), Math.round(g / 17.0), Math.round(bl / 17.0));
    }

    private static int[] parseRgb(String hex) {
        if (Obj.isBlank(hex) || !hex.startsWith("#")) {
            return new int[] { 128, 128, 128 };
        }
        String h = hex.substring(1);
        if (h.length() == 3) {
            return new int[] {
                    Integer.parseInt(h.substring(0, 1), 16) * 17,
                    Integer.parseInt(h.substring(1, 2), 16) * 17,
                    Integer.parseInt(h.substring(2, 3), 16) * 17
            };
        }
        if (h.length() == 6) {
            return new int[] {
                    Integer.parseInt(h.substring(0, 2), 16),
                    Integer.parseInt(h.substring(2, 4), 16),
                    Integer.parseInt(h.substring(4, 6), 16)
            };
        }
        return new int[] { 128, 128, 128 };
    }

    private static String encodeSvgDataUri(String svg) {
        // 百分号编码比 base64(+33%) 更省, SVG 是纯 ASCII, 只需转义少数字符
        return svg.replace("\"", "'").replace("%", "%25").replace("#", "%23")
                .replace("<", "%3C").replace(">", "%3E");
    }

    private static String escapeXml(String src) {
        return src.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }



    private static final class GlyphPaint {
        private final Captcha.Glyph glyph;
        private final String value;
        private final int cx;
        private final int cy;
        private final String fill;
        private final int fontSize;
        private final int rotate;

        private GlyphPaint(Captcha.Glyph glyph, String value, int cx, int cy, String fill, int fontSize, int rotate) {
            this.glyph = glyph;
            this.value = value;
            this.cx = cx;
            this.cy = cy;
            this.fill = fill;
            this.fontSize = fontSize;
            this.rotate = rotate;
        }

        private Captcha.Glyph glyph() {
            return glyph;
        }

        private String value() {
            return value;
        }

        private int cx() {
            return cx;
        }

        private int cy() {
            return cy;
        }

        private String fill() {
            return fill;
        }

        private int fontSize() {
            return fontSize;
        }

        private int rotate() {
            return rotate;
        }
    }
}
