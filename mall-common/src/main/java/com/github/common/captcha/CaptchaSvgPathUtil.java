package com.github.common.captcha;

import com.github.common.util.Arr;
import com.github.common.util.LogUtil;
import com.github.common.util.Obj;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中文点选验证码(单张 SVG Path 化).
 * 布局: 上方提示区(明文提示) + 下方点击区(字形 Path 化).
 * 需服务器有真实中文字体; 无可用字体时由 CaptchaHandler 降级 CaptchaSvgUtil(客户端渲染).
 */
@SuppressWarnings("DuplicatedCode")
public class CaptchaSvgPathUtil {

    private static final FontRenderContext FONT_RENDER_CONTEXT = new FontRenderContext(null, true, true);
    /** 未变形轮廓缓存, 避免重复矢量轮廓 */
    private static final ConcurrentHashMap<String, Shape> OUTLINE_CACHE = new ConcurrentHashMap<>();
    /** Path 展平精度: 越大段越少越快, 验证码观感仍可接受 */
    private static final double PATH_FLATNESS = 0.75;

    /**
     * @param dark 可选, 为 1/true/on(忽略大小写) 时使用深色主题; 否则浅色
     */
    public static Captcha.Build buildClickCaptcha(String width, String height, String dark) {
        int imageWidth = Captcha.resolveWidth(width);
        int imageHeight = Captcha.resolveHeight(height);
        CaptchaTheme theme = randomTheme(Obj.toBool(dark));

        int targetCount = Captcha.randomTargetCount();
        int noiseCount = Captcha.randomNoiseCount(targetCount);
        List<String> targetChars = Captcha.pickRenderableTargets(targetCount);
        List<String> noiseChars = Captcha.pickRenderableNoise(targetChars, noiseCount);

        int promptBottom = Captcha.promptBottom(imageHeight);
        int clickHeight = imageHeight - promptBottom;

        List<SvgPath> pathList = new ArrayList<>();
        List<Captcha.Glyph> glyphList = new ArrayList<>();
        int totalGlyphCount = targetChars.size() + noiseChars.size();
        int clickFontSize = Math.max(15, Math.min(20, clickHeight / 3));
        List<Captcha.Point> points = Captcha.randomPoints(imageWidth, clickHeight, clickFontSize, totalGlyphCount);
        int pointIndex = 0;
        for (int i = 0; i < targetChars.size(); i++) {
            Captcha.Point point = points.get(pointIndex++);
            int absY = point.y() + promptBottom;
            GlyphBuild built = buildClickGlyph(targetChars.get(i), point.x(), absY, true, i,
                    clickFontSize, imageWidth, promptBottom, imageHeight, theme);
            glyphList.add(built.glyph());
            pathList.addAll(built.paths());
        }
        for (String noiseChar : noiseChars) {
            Captcha.Point point = points.get(pointIndex++);
            int absY = point.y() + promptBottom;
            GlyphBuild built = buildClickGlyph(noiseChar, point.x(), absY, false, -1,
                    clickFontSize, imageWidth, promptBottom, imageHeight, theme);
            glyphList.add(built.glyph());
            pathList.addAll(built.paths());
        }
        // 分隔线; 字形已 Path 化并逐字形变, 干扰线对机器无效且影响人读, 不再输出
        pathList.add(buildDividerPath(imageWidth, promptBottom, theme));

        Collections.shuffle(pathList, Obj.RANDOM);
        // 提示目标字用明文 text(不变形), 避免与下方点击区混淆; 也少做几次 outline
        String svgText = buildSvgDocument(imageWidth, imageHeight, theme, pathList, targetChars, promptBottom);
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("生成的 svg({})", svgText);
        }
        // 百分号编码比 base64(+33%) 更省, SVG 是纯 ASCII, 只需转义少数字符(约 +5%)
        String dataUri = "data:image/svg+xml," + encodeSvgDataUri(svgText);
        Captcha.Challenge challenge = new Captcha.Challenge(targetChars, glyphList, imageWidth, imageHeight, promptBottom);
        return new Captcha.Build(dataUri, imageWidth, imageHeight, challenge);
    }

    private static GlyphBuild buildClickGlyph(
            String value,
            int cx,
            int cy,
            boolean target,
            int targetOrder,
            int baseFontSize,
            int imageWidth,
            int clickTop,
            int imageHeight,
            CaptchaTheme theme
    ) {
        // 字号浮动 -3 ~ +5, 大小差异更明显
        int fontSize = baseFontSize + Obj.RANDOM.nextInt(9) - 3;
        fontSize = Math.max(13, Math.min(28, fontSize));
        Font font = Captcha.pickFont(fontSize, true);
        // 透明度直接与背景色预混合成最终色, SVG 中不再输出 opacity 属性
        double opacity = 0.72 + Obj.RANDOM.nextDouble() * 0.28;
        String fill = blendHexColor(randomGlyphColor(theme), theme.bgColor(), opacity);
        float strokeWidth = Obj.RANDOM.nextInt(100) < 30 ? 0.4f + Obj.RANDOM.nextFloat() * 0.35f : 0f;
        String stroke = strokeWidth > 0 ? adjustHexColor(fill, theme.dark() ? -40 : 36) : null;
        GlyphBuild built = outlineToPaths(value, font, cx, cy,
                randomInRange(0.76, 1.24), randomInRange(0.80, 1.26),
                randomInRange(-24, 24), randomInRange(-0.25, 0.25),
                fill, strokeWidth, stroke);
        // 命中半径按字号估计, 并夹在图内
        int radius = Math.max(10, fontSize / 2 + 4);
        int safeX = Math.min(imageWidth - 4, Math.max(4, cx));
        int safeY = Math.min(imageHeight - 4, Math.max(clickTop + 4, cy));
        Captcha.Glyph glyph = new Captcha.Glyph(value, safeX, safeY, radius, target, targetOrder);
        return new GlyphBuild(glyph, built.paths());
    }

    private static GlyphBuild outlineToPaths(
            String value,
            Font font,
            double targetCx,
            double targetCy,
            double scaleX,
            double scaleY,
            double rotateDeg,
            double shearX,
            String fillColor,
            float strokeWidth,
            String strokeColor
    ) {
        Shape outline = cachedOutline(font, value);
        Rectangle2D bounds = outline.getBounds2D();
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return new GlyphBuild(null, List.of());
        }
        AffineTransform at = new AffineTransform();
        at.translate(targetCx, targetCy);
        at.rotate(Math.toRadians(rotateDeg));
        at.shear(shearX, 0);
        at.scale(scaleX, scaleY);
        at.translate(-bounds.getCenterX(), -bounds.getCenterY());

        List<String> dList = pathIteratorToDList(outline.getPathIterator(at, PATH_FLATNESS));
        List<SvgPath> paths = new ArrayList<>(1);
        // 单字全部轮廓并成一条 path: evenodd 挖孔仍然正确, 且少重复 fill/stroke 属性
        StringBuilder merged = new StringBuilder(256);
        for (String d : dList) {
            if (Obj.isNotBlank(d)) {
                merged.append(d);
            }
        }
        if (!merged.isEmpty()) {
            paths.add(new SvgPath(merged.toString(), fillColor, strokeWidth, strokeColor));
        }
        return new GlyphBuild(null, paths);
    }

    /** 单条轮廓: d 串 + 包围盒(用于孔洞归组; 现已整字合并, 保留盒信息便于后续若再拆分) */
    private static final class Contour {
        private final String d;
        private final double minX;
        private final double minY;
        private final double maxX;
        private final double maxY;

        private Contour(String d, double minX, double minY, double maxX, double maxY) {
            this.d = d;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        private String d() {
            return d;
        }

        private double area() {
            return Math.max(0, maxX - minX) * Math.max(0, maxY - minY);
        }

        private boolean containsBox(Contour other) {
            return minX <= other.minX && minY <= other.minY && maxX >= other.maxX && maxY >= other.maxY;
        }
    }

    private static List<String> pathIteratorToDList(PathIterator iterator) {
        List<Contour> contours = collectContours(iterator);
        if (contours.isEmpty()) {
            return List.of();
        }
        // 孔洞与外轮廓最终会并进同一 path, 这里仍按包含关系归组, 保证输出顺序稳定
        int n = contours.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            double bestArea = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    continue;
                }
                Contour cj = contours.get(j);
                if (cj.containsBox(contours.get(i)) && cj.area() > contours.get(i).area() && cj.area() < bestArea) {
                    bestArea = cj.area();
                    parent[i] = j;
                }
            }
        }
        Map<Integer, StringBuilder> groups = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int root = i;
            int guard = 0;
            while (parent[root] != root && guard++ < n) {
                root = parent[root];
            }
            //noinspection unused
            groups.computeIfAbsent(root, key -> new StringBuilder(160)).append(contours.get(i).d());
        }
        List<String> result = new ArrayList<>(groups.size());
        for (StringBuilder builder : groups.values()) {
            result.add(builder.toString());
        }
        return result;
    }

    private static List<Contour> collectContours(PathIterator iterator) {
        List<Contour> contours = new ArrayList<>();
        StringBuilder current = new StringBuilder(128);
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        // 坐标统一换算到 0.1px 整数(tenths)后再算相对量, 避免浮点累积误差
        long curX = 0;
        long curY = 0;
        long startX = 0;
        long startY = 0;
        double[] coords = new double[6];
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            int pointCount = switch (type) {
                case PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO -> 1;
                case PathIterator.SEG_QUADTO -> 2;
                case PathIterator.SEG_CUBICTO -> 3;
                default -> 0;
            };
            if (type == PathIterator.SEG_MOVETO && !current.isEmpty()) {
                contours.add(new Contour(current.toString(), minX, minY, maxX, maxY));
                current = new StringBuilder(128);
                minX = Double.MAX_VALUE;
                minY = Double.MAX_VALUE;
                maxX = -Double.MAX_VALUE;
                maxY = -Double.MAX_VALUE;
            }
            switch (type) {
                case PathIterator.SEG_MOVETO -> {
                    long x = px(coords[0]);
                    long y = px(coords[1]);
                    current.append('M');
                    appendPx(current, x, false);
                    appendPx(current, y, true);
                    curX = x;
                    curY = y;
                    startX = x;
                    startY = y;
                }
                case PathIterator.SEG_LINETO -> {
                    long x = px(coords[0]);
                    long y = px(coords[1]);
                    // 零位移段直接丢掉, 省字节且不影响外形
                    if (x == curX && y == curY) {
                        break;
                    }
                    // 横平竖直的笔画用 h/v, 比 l 再省一个数
                    if (y == curY) {
                        current.append('h');
                        appendPx(current, x - curX, false);
                    } else if (x == curX) {
                        current.append('v');
                        appendPx(current, y - curY, false);
                    } else {
                        current.append('l');
                        appendPx(current, x - curX, false);
                        appendPx(current, y - curY, true);
                    }
                    curX = x;
                    curY = y;
                }
                case PathIterator.SEG_QUADTO -> {
                    long x1 = px(coords[0]);
                    long y1 = px(coords[1]);
                    long x = px(coords[2]);
                    long y = px(coords[3]);
                    current.append('q');
                    appendPx(current, x1 - curX, false);
                    appendPx(current, y1 - curY, true);
                    appendPx(current, x - curX, true);
                    appendPx(current, y - curY, true);
                    curX = x;
                    curY = y;
                }
                case PathIterator.SEG_CUBICTO -> {
                    long x1 = px(coords[0]);
                    long y1 = px(coords[1]);
                    long x2 = px(coords[2]);
                    long y2 = px(coords[3]);
                    long x = px(coords[4]);
                    long y = px(coords[5]);
                    current.append('c');
                    appendPx(current, x1 - curX, false);
                    appendPx(current, y1 - curY, true);
                    appendPx(current, x2 - curX, true);
                    appendPx(current, y2 - curY, true);
                    appendPx(current, x - curX, true);
                    appendPx(current, y - curY, true);
                    curX = x;
                    curY = y;
                }
                case PathIterator.SEG_CLOSE -> {
                    current.append('z');
                    curX = startX;
                    curY = startY;
                }
                default -> {
                }
            }
            for (int p = 0; p < pointCount; p++) {
                double x = coords[p * 2];
                double y = coords[p * 2 + 1];
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
            iterator.next();
        }
        if (!current.isEmpty()) {
            contours.add(new Contour(current.toString(), minX, minY, maxX, maxY));
        }
        return contours;
    }

    /** 路径坐标量化到整像素, 比 0.1px 短很多且 224 宽下肉眼难辨 */
    private static long px(double v) {
        return Math.round(v);
    }

    /** needSep 为 true 时在非负数前补空格分隔(负号本身就是分隔) */
    private static void appendPx(StringBuilder sb, long value, boolean needSep) {
        if (needSep && value >= 0) {
            sb.append(' ');
        }
        sb.append(value);
    }

    /** stroke-width 仍用 0.1px, 细线需要小数 */
    private static void appendTenths(StringBuilder sb, double value) {
        long tenthsValue = Math.round(value * 10.0);
        long abs = Math.abs(tenthsValue);
        long intPart = abs / 10;
        long frac = abs % 10;
        if (tenthsValue < 0) {
            sb.append('-');
        }
        if (intPart == 0 && frac != 0) {
            sb.append('.').append(frac);
        } else {
            sb.append(intPart);
            if (frac != 0) {
                sb.append('.').append(frac);
            }
        }
    }

    private static SvgPath buildDividerPath(int width, int y, CaptchaTheme theme) {
        String d = "M0 " + y + "h" + width;
        return new SvgPath(d, "none", 1f, theme.dividerColor());
    }

    private static String buildSvgDocument(
            int width, int height, CaptchaTheme theme, List<SvgPath> pathList,
            List<String> promptTargets, int promptBottom
    ) {
        StringBuilder svg = new StringBuilder(Math.max(4096, pathList.size() * 140));
        // fill-rule 写在根节点由子元素继承, 保证孔洞轮廓正确挖空且不依赖轮廓方向
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
                .append("\" height=\"").append(height)
                .append("\" viewBox=\"0 0 ").append(width).append(' ').append(height)
                .append("\" fill-rule=\"evenodd\">");
        svg.append("<rect width=\"").append(width).append("\" height=\"").append(height)
                .append("\" fill=\"").append(theme.bgColor()).append("\"/>");
        // 提示文案用小字; 目标字明文 + 轻旋转, 与下方 Path 点击区仍能区分
        int promptBaseY = Math.max(14, Captcha.PROMPT_AREA_HEIGHT * 20 / 32);
        svg.append("<text x=\"6\" y=\"").append(promptBaseY)
                .append("\" font-size=\"10\" fill=\"")
                .append(theme.secondaryColor()).append("\">请在下方依次点击</text>");
        if (Arr.isNotEmpty(promptTargets)) {
            int promptFont = Math.max(14, Math.min(18, promptBottom - 8));
            double rightPad = 6;
            // 左侧小字约占 90px, 其余给目标字
            double maxBlockW = Math.max(40, width - 90 - rightPad);
            double slotW = Math.min(promptFont + 4, maxBlockW / promptTargets.size());
            promptFont = (int) Math.max(12, Math.min(promptFont, slotW - 1));
            double blockW = promptTargets.size() * slotW;
            double startX = width - rightPad - blockW;
            double cy = promptBottom / 2.0;
            for (int i = 0; i < promptTargets.size(); i++) {
                double cx = startX + (i + 0.5) * slotW;
                int rotate = randomInRange(-15, 15);
                int tx = (int) Math.round(cx - promptFont / 2.0);
                int ty = (int) Math.round(cy + promptFont * 0.35);
                svg.append("<text x=\"").append(tx).append("\" y=\"").append(ty)
                        .append("\" font-size=\"").append(promptFont)
                        .append("\" font-weight=\"700\" fill=\"")
                        .append(theme.primaryColor()).append('"');
                if (rotate != 0) {
                    svg.append(" transform=\"rotate(").append(rotate).append(' ')
                            .append(Math.round(cx)).append(' ').append(Math.round(cy)).append(")\"");
                }
                svg.append('>').append(escapeXml(promptTargets.get(i))).append("</text>");
            }
        }
        for (SvgPath path : pathList) {
            svg.append("<path d=\"").append(path.d()).append('"');
            svg.append(" fill=\"").append(path.fill()).append('"');
            if (path.strokeWidth() > 0 && Obj.isNotBlank(path.stroke())) {
                svg.append(" stroke=\"").append(path.stroke()).append('"');
                if (path.strokeWidth() != 1f) {
                    svg.append(" stroke-width=\"");
                    appendTenths(svg, path.strokeWidth());
                    svg.append('"');
                }
            }
            svg.append("/>");
        }
        svg.append("</svg>");
        return svg.toString();
    }

    private static String escapeXml(String value) {
        if (Obj.isBlank(value)) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String randomGlyphColor(CaptchaTheme theme) {
        double h = Obj.RANDOM.nextDouble();
        if (theme.dark()) {
            return hsvToHex(h, 0.05 + Obj.RANDOM.nextDouble() * 0.50, 0.72 + Obj.RANDOM.nextDouble() * 0.28);
        }
        return hsvToHex(h, 0.22 + Obj.RANDOM.nextDouble() * 0.60, 0.18 + Obj.RANDOM.nextDouble() * 0.44);
    }




    private static String hsvToHex(double h, double s, double v) {
        double hh = (h % 1.0 + 1.0) % 1.0;
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
        int r = clamp255((int) Math.round((r1 + m) * 255));
        int g = clamp255((int) Math.round((g1 + m) * 255));
        int b = clamp255((int) Math.round((b1 + m) * 255));
        return String.format(Locale.ROOT, "#%02x%02x%02x", r, g, b);
    }

    /** img src 里的 SVG 数据 URI 只需转义这几个字符; 双引号换成单引号免转义 */
    private static String encodeSvgDataUri(String svg) {
        return svg.replace("\"", "'")
                .replace("%", "%25")
                .replace("#", "%23")
                .replace("<", "%3C")
                .replace(">", "%3E");
    }

    /**
     * 按不透明度将前景色与背景色预混合, 并量化到 #rgb 短格式(每通道舍入到 17 的倍数), 每条 path 省 3 字节.
     */
    private static String blendHexColor(String fgHex, String bgHex, double alpha) {
        if (Obj.isBlank(fgHex) || fgHex.length() != 7 || Obj.isBlank(bgHex) || bgHex.length() != 7) {
            return fgHex;
        }
        double a = Math.max(0, Math.min(1, alpha));
        int r = (int) Math.round(parseHex(fgHex.substring(1, 3)) * a + parseHex(bgHex.substring(1, 3)) * (1 - a));
        int g = (int) Math.round(parseHex(fgHex.substring(3, 5)) * a + parseHex(bgHex.substring(3, 5)) * (1 - a));
        int b = (int) Math.round(parseHex(fgHex.substring(5, 7)) * a + parseHex(bgHex.substring(5, 7)) * (1 - a));
        return String.format("#%x%x%x",
                Math.round(clamp255(r) / 17.0), Math.round(clamp255(g) / 17.0), Math.round(clamp255(b) / 17.0));
    }

    /** 支持 #rgb 和 #rrggbb 两种输入, 输出 #rgb 短格式 */
    private static String adjustHexColor(String hexColor, int delta) {
        if (Obj.isBlank(hexColor) || !hexColor.startsWith("#")) {
            return hexColor;
        }
        int r;
        int g;
        int b;
        if (hexColor.length() == 4) {
            r = parseHex(hexColor.substring(1, 2)) * 17;
            g = parseHex(hexColor.substring(2, 3)) * 17;
            b = parseHex(hexColor.substring(3, 4)) * 17;
        } else if (hexColor.length() == 7) {
            r = parseHex(hexColor.substring(1, 3));
            g = parseHex(hexColor.substring(3, 5));
            b = parseHex(hexColor.substring(5, 7));
        } else {
            return hexColor;
        }
        return String.format("#%x%x%x",
                Math.round(clamp255(r + delta) / 17.0), Math.round(clamp255(g + delta) / 17.0), Math.round(clamp255(b + delta) / 17.0));
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

    private static double randomInRange(double min, double max) {
        return min + Obj.RANDOM.nextDouble() * (max - min);
    }

    private static int randomInRange(int min, int maxInclusive) {
        return min + Obj.RANDOM.nextInt(maxInclusive - min + 1);
    }

    private static Shape cachedOutline(Font font, String value) {
        String key = font.getName() + "|" + font.getSize() + "|" + value;
        //noinspection unused
        return OUTLINE_CACHE.computeIfAbsent(key, k -> {
            GlyphVector gv = font.createGlyphVector(FONT_RENDER_CONTEXT, value);
            return new GeneralPath(gv.getOutline());
        });
    }


    private static CaptchaTheme randomTheme(boolean dark) {
        double h = Obj.RANDOM.nextDouble();
        if (dark) {
            String bg = hsvToHex(h, 0.02 + Obj.RANDOM.nextDouble() * 0.14, 0.06 + Obj.RANDOM.nextDouble() * 0.12);
            String primary = hsvToHex(h, 0.02 + Obj.RANDOM.nextDouble() * 0.06, 0.88 + Obj.RANDOM.nextDouble() * 0.10);
            String secondary = hsvToHex(h, 0.03 + Obj.RANDOM.nextDouble() * 0.08, 0.55 + Obj.RANDOM.nextDouble() * 0.14);
            String divider = hsvToHex(h, 0.03 + Obj.RANDOM.nextDouble() * 0.08, 0.20 + Obj.RANDOM.nextDouble() * 0.10);
            return new CaptchaTheme(true, bg, primary, secondary, divider);
        }
        String bg = hsvToHex(h, 0.02 + Obj.RANDOM.nextDouble() * 0.12, 0.92 + Obj.RANDOM.nextDouble() * 0.07);
        String primary = hsvToHex(h, 0.06 + Obj.RANDOM.nextDouble() * 0.14, 0.20 + Obj.RANDOM.nextDouble() * 0.14);
        String secondary = hsvToHex(h, 0.04 + Obj.RANDOM.nextDouble() * 0.10, 0.40 + Obj.RANDOM.nextDouble() * 0.12);
        String divider = hsvToHex(h, 0.03 + Obj.RANDOM.nextDouble() * 0.08, 0.76 + Obj.RANDOM.nextDouble() * 0.10);
        return new CaptchaTheme(false, bg, primary, secondary, divider);
    }

    private static final class CaptchaTheme {
        private final boolean dark;
        private final String bgColor;
        private final String primaryColor;
        private final String secondaryColor;
        private final String dividerColor;

        private CaptchaTheme(boolean dark, String bgColor, String primaryColor, String secondaryColor, String dividerColor) {
            this.dark = dark;
            this.bgColor = bgColor;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.dividerColor = dividerColor;
        }

        private boolean dark() {
            return dark;
        }

        private String bgColor() {
            return bgColor;
        }

        private String primaryColor() {
            return primaryColor;
        }

        private String secondaryColor() {
            return secondaryColor;
        }

        private String dividerColor() {
            return dividerColor;
        }
    }

    private static final class SvgPath {
        private final String d;
        private final String fill;
        private final float strokeWidth;
        private final String stroke;

        private SvgPath(String d, String fill, float strokeWidth, String stroke) {
            this.d = d;
            this.fill = fill;
            this.strokeWidth = strokeWidth;
            this.stroke = stroke;
        }

        private String d() {
            return d;
        }

        private String fill() {
            return fill;
        }

        private float strokeWidth() {
            return strokeWidth;
        }

        private String stroke() {
            return stroke;
        }
    }

    private static final class GlyphBuild {
        private final Captcha.Glyph glyph;
        private final List<SvgPath> paths;

        private GlyphBuild(Captcha.Glyph glyph, List<SvgPath> paths) {
            this.glyph = glyph;
            this.paths = paths;
        }

        private Captcha.Glyph glyph() {
            return glyph;
        }

        private List<SvgPath> paths() {
            return paths;
        }
    }
}
