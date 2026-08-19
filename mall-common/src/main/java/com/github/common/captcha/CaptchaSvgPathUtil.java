package com.github.common.captcha;

import com.github.common.util.Arr;
import com.github.common.util.LogUtil;
import com.github.common.util.Obj;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * 中文点选验证码(单张 SVG Path 化).
 * 布局: 上方提示区 + 下方点击区; 全部字形输出为 path, 不含 text/transform.
 * 需服务器有真实中文字体; 无可用字体时由 CaptchaHandler 降级 CaptchaSvgUtil(客户端渲染).
 */
@SuppressWarnings("DuplicatedCode")
public final class CaptchaSvgPathUtil {

    /** GET /captcha 未传 width 时的默认像素 */
    public static final int DEFAULT_CAPTCHA_WIDTH = 224;
    private static final int CAPTCHA_WIDTH_MIN = 200;
    private static final int CAPTCHA_WIDTH_MAX = 400;
    /** GET /captcha 未传 height 时的默认像素(含提示区) */
    public static final int DEFAULT_CAPTCHA_HEIGHT = 88;
    private static final int CAPTCHA_HEIGHT_MIN = 72;
    private static final int CAPTCHA_HEIGHT_MAX = 140;
    /** 提示区高度(像素), 与前端约定: 仅允许点击此线以下 */
    public static final int PROMPT_AREA_HEIGHT = 32;

    /** 目标字数每次在 [MIN, MAX] 随机; 数字白名单是 3 字条目, 目标数不为 3 时数字类自动不参与 */
    public static final int TARGET_COUNT_MIN = CaptchaChars.TARGET_COUNT_MIN;
    public static final int TARGET_COUNT_MAX = CaptchaChars.TARGET_COUNT_MAX;
    /** 干扰字数每次随机, 下限保底混淆, 上限受总字形数约束 */
    private static final int NOISE_COUNT_MIN = 2;
    /** 点击区总字形数(目标+干扰)上限, 默认 224 宽下超过 7 个会开始拥挤 */
    private static final int GLYPH_TOTAL_MAX = 7;
    private static final int DEFAULT_CLICK_TOLERANCE_PX = 12;

    /** 启动时探测本机可显示中文的字体族(不含 Dialog/DejaVu 等伪支持) */
    private static final List<String> GLYPH_FONT_FAMILIES = resolveGlyphFontFamilies();
    /** 当前字体可显示的干扰字池 / 同类字池 / 数字白名单 */
    private static final String RENDERABLE_NOISE_POOL;
    private static final List<String> RENDERABLE_GROUPS;
    private static final List<String> RENDERABLE_NUMBER_TRIPLETS;
    static {
        Font font = pickFont(18, false);
        RENDERABLE_NOISE_POOL = CaptchaChars.filterNoisePool(font);
        RENDERABLE_GROUPS = CaptchaChars.filterGroups(font);
        RENDERABLE_NUMBER_TRIPLETS = CaptchaChars.filterNumberTriplets(font);
    }
    private static final FontRenderContext FONT_RENDER_CONTEXT = new FontRenderContext(null, true, true);

    private CaptchaSvgPathUtil() {
    }

    /** 是否有真实中文字体可做 path; 否则应降级 CaptchaSvgUtil */
    public static boolean canRenderPathGlyphs() {
        return Arr.isNotEmpty(GLYPH_FONT_FAMILIES);
    }

    /**
     * @param dark 可选, 为 1/true/on(忽略大小写) 时使用深色主题; 否则浅色
     */
    public static CaptchaRecord.Build buildClickCaptcha(String width, String height, String dark) {
        int imageWidth = resolveCaptchaWidthOrHeight(width, DEFAULT_CAPTCHA_WIDTH, CAPTCHA_WIDTH_MIN, CAPTCHA_WIDTH_MAX);
        int imageHeight = resolveCaptchaWidthOrHeight(height, DEFAULT_CAPTCHA_HEIGHT, CAPTCHA_HEIGHT_MIN, CAPTCHA_HEIGHT_MAX);
        CaptchaTheme theme = randomTheme(Obj.toBool(dark));

        int targetCount = TARGET_COUNT_MIN + Obj.RANDOM.nextInt(TARGET_COUNT_MAX - TARGET_COUNT_MIN + 1);
        int noiseCount = NOISE_COUNT_MIN + Obj.RANDOM.nextInt(GLYPH_TOTAL_MAX - targetCount - NOISE_COUNT_MIN + 1);
        List<String> targetChars = CaptchaChars.pickTargets(targetCount, RENDERABLE_GROUPS, RENDERABLE_NUMBER_TRIPLETS);
        List<String> noiseChars = CaptchaChars.pickNoise(targetChars, noiseCount, RENDERABLE_NOISE_POOL);

        // 提示区高度固定, 与前端 CAPTCHA_PROMPT_AREA_HEIGHT 保持一致
        int promptBottom = Math.min(PROMPT_AREA_HEIGHT, Math.max(24, imageHeight - 40));
        int clickHeight = imageHeight - promptBottom;

        List<SvgPath> pathList = new ArrayList<>();
        // 固定文案「请依次点击」不含秘密, 图内画提示目标字(与点击区同字但独立形变, 禁止复用 path)
        appendPromptGlyphPaths(pathList, targetChars, theme, imageWidth, promptBottom);
        // 点击区: targetCount 目标 + noiseCount 干扰(个数均已随机); 前端会把图拉伸到容器宽, 字号偏小一档显示才合适
        List<CaptchaRecord.Glyph> glyphList = new ArrayList<>();
        int totalGlyphCount = targetChars.size() + noiseChars.size();
        int clickFontSize = Math.max(15, Math.min(20, clickHeight / 3));
        List<CaptchaRecord.Point> points = randomPoints(imageWidth, clickHeight, clickFontSize, totalGlyphCount);
        int pointIndex = 0;
        for (int i = 0; i < targetChars.size(); i++) {
            CaptchaRecord.Point point = points.get(pointIndex++);
            int absY = point.y() + promptBottom;
            GlyphBuild built = buildClickGlyph(targetChars.get(i), point.x(), absY, true, i,
                    clickFontSize, imageWidth, promptBottom, imageHeight, theme);
            glyphList.add(built.glyph());
            pathList.addAll(built.paths());
        }
        for (String noiseChar : noiseChars) {
            CaptchaRecord.Point point = points.get(pointIndex++);
            int absY = point.y() + promptBottom;
            GlyphBuild built = buildClickGlyph(noiseChar, point.x(), absY, false, -1,
                    clickFontSize, imageWidth, promptBottom, imageHeight, theme);
            glyphList.add(built.glyph());
            pathList.addAll(built.paths());
        }
        // 分隔线; 字形已 Path 化并逐字形变, 干扰线对机器无效且影响人读, 不再输出
        pathList.add(buildDividerPath(imageWidth, promptBottom, theme));

        Collections.shuffle(pathList, Obj.RANDOM);
        String svgText = buildSvgDocument(imageWidth, imageHeight, theme, pathList);
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("生成的 svg({})", svgText);
        }
        // 百分号编码比 base64(+33%) 更省, SVG 是纯 ASCII, 只需转义少数字符(约 +5%)
        String dataUri = "data:image/svg+xml," + encodeSvgDataUri(svgText);
        CaptchaRecord.Challenge challenge = new CaptchaRecord.Challenge(targetChars, glyphList, imageWidth, imageHeight, promptBottom);
        return new CaptchaRecord.Build(dataUri, imageWidth, imageHeight, challenge);
    }

    private static void appendPromptGlyphPaths(
            List<SvgPath> pathList, List<String> targetChars, CaptchaTheme theme, int imageWidth, int promptBottom
    ) {
        int fontSize = Math.max(14, Math.min(18, promptBottom - 8));
        // 固定文案在左侧(text 节点, 约占 80px), 提示目标字靠右; 目标数变多时按剩余宽度压缩槽位和字号
        double rightPad = 8;
        double maxBlockW = Math.max(40, imageWidth - 84 - rightPad);
        double slotW = Math.min(fontSize + 6, maxBlockW / targetChars.size());
        fontSize = (int) Math.max(11, Math.min(fontSize, slotW - 2));
        double blockW = targetChars.size() * slotW;
        double startX = imageWidth - rightPad - blockW;
        double cy = promptBottom / 2.0;
        for (int i = 0; i < targetChars.size(); i++) {
            Font font = pickFont(fontSize, true);
            double cx = startX + (i + 0.5) * slotW;
            // 提示字与点击区目标字必须分别形变; 提示字是人读参照, 幅度小于点击区
            GlyphBuild built = outlineToPaths(targetChars.get(i), font, cx, cy,
                    randomInRange(0.84, 1.12), randomInRange(0.86, 1.14),
                    randomInRange(-15, 15), randomInRange(-0.16, 0.16),
                    blendHexColor(theme.primaryColor(), theme.bgColor(), 0.95), 0f, null);
            pathList.addAll(built.paths());
        }
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
        Font font = pickFont(fontSize, true);
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
        CaptchaRecord.Glyph glyph = new CaptchaRecord.Glyph(value, safeX, safeY, radius, target, targetOrder);
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
        GlyphVector gv = font.createGlyphVector(FONT_RENDER_CONTEXT, value);
        Shape outline = gv.getOutline();
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

        List<String> dList = pathIteratorToDList(outline.getPathIterator(at));
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
    private record Contour(String d, double minX, double minY, double maxX, double maxY) {
        double area() {
            return Math.max(0, maxX - minX) * Math.max(0, maxY - minY);
        }

        boolean containsBox(Contour other) {
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

    private static String buildSvgDocument(int width, int height, CaptchaTheme theme, List<SvgPath> pathList) {
        StringBuilder svg = new StringBuilder(Math.max(4096, pathList.size() * 140));
        // fill-rule 写在根节点由子元素继承, 保证孔洞轮廓正确挖空且不依赖轮廓方向
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
                .append("\" height=\"").append(height)
                .append("\" viewBox=\"0 0 ").append(width).append(' ').append(height)
                .append("\" fill-rule=\"evenodd\">");
        svg.append("<rect width=\"").append(width).append("\" height=\"").append(height)
                .append("\" fill=\"").append(theme.bgColor()).append("\"/>");
        // 固定公开文案不含秘密, 用 text 节点即可, 比 Path 化省 2KB 左右
        svg.append("<text x=\"10\" y=\"").append(Math.max(16, PROMPT_AREA_HEIGHT * 21 / 32))
                .append("\" font-size=\"13\" fill=\"")
                .append(theme.secondaryColor()).append("\">请依次点击</text>");
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

    private static String randomGlyphColor(CaptchaTheme theme) {
        double h = Obj.RANDOM.nextDouble();
        if (theme.dark()) {
            return hsvToHex(h, 0.05 + Obj.RANDOM.nextDouble() * 0.50, 0.72 + Obj.RANDOM.nextDouble() * 0.28);
        }
        return hsvToHex(h, 0.22 + Obj.RANDOM.nextDouble() * 0.60, 0.18 + Obj.RANDOM.nextDouble() * 0.44);
    }


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

    private static List<CaptchaRecord.Point> randomPoints(int width, int height, int fontSize, int count) {
        int padX = Math.min(fontSize + 14, Math.max(10, width / 5));
        int padY = Math.min(fontSize / 2 + 6, Math.max(8, height / 3));
        if (width <= padX * 2 + 4) {
            padX = Math.max(6, width / 6);
        }
        if (height <= padY * 2 + 4) {
            padY = Math.max(6, height / 5);
        }
        int innerW = width - padX * 2;
        int innerH = height - padY * 2;
        List<CaptchaRecord.Point> pointList = new ArrayList<>(count);
        if (innerW < 1 || innerH < 1) {
            return pointList;
        }
        double halfSpanX = fontSize * 0.55;
        double cellW = innerW / (double) count;
        double maxJitterX = cellW / 2.0 - halfSpanX - 2;
        if (maxJitterX < 0) {
            maxJitterX = 0;
        }
        List<Integer> slots = new ArrayList<>(count);
        for (int s = 0; s < count; s++) {
            slots.add(s);
        }
        Collections.shuffle(slots, Obj.RANDOM);
        for (int gi = 0; gi < count; gi++) {
            int slot = slots.get(gi);
            double baseX = padX + (slot + 0.5) * cellW;
            double jitterX = maxJitterX <= 0 ? 0 : (Obj.RANDOM.nextDouble() * 2 - 1) * maxJitterX;
            int x = (int) Math.round(baseX + jitterX);
            x = Math.min(width - padX - 1, Math.max(padX, x));
            int y = padY + Obj.RANDOM.nextInt(innerH);
            pointList.add(new CaptchaRecord.Point(x, y));
        }
        return pointList;
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

    private static Font pickFont(int size, boolean preferAlternate) {
        if (Arr.isEmpty(GLYPH_FONT_FAMILIES)) {
            return new Font(Font.DIALOG, Font.PLAIN, size);
        }
        int idx = 0;
        if (preferAlternate && GLYPH_FONT_FAMILIES.size() > 1) {
            idx = 1 + Obj.RANDOM.nextInt(GLYPH_FONT_FAMILIES.size() - 1);
        } else if (GLYPH_FONT_FAMILIES.size() > 1 && Obj.RANDOM.nextBoolean()) {
            idx = Obj.RANDOM.nextInt(GLYPH_FONT_FAMILIES.size());
        }
        return new Font(GLYPH_FONT_FAMILIES.get(idx), Font.PLAIN, size);
    }

    private static List<String> resolveGlyphFontFamilies() {
        List<String> resolved = new ArrayList<>();
        final String probe = "验证码春夏秋";
        // 仅真实 CJK; 不含 Dialog/DejaVu/Nimbus 等(canDisplay 可能撒谎或轮廓是方框).
        // 最小安装(装完需重启 JVM): yum install -y wqy-microhei-fonts / apt install -y fonts-wqy-microhei
        String[] candidates = {
                "Noto Sans CJK SC", "Source Han Sans SC", "Microsoft YaHei",
                "WenQuanYi Zen Hei", "WenQuanYi Micro Hei", "PingFang SC",
                "Hiragino Sans GB", "SimHei", "SimSun", "Droid Sans Fallback"
        };
        try {
            String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            Set<String> familySet = new HashSet<>();
            for (String family : families) {
                familySet.add(family.toLowerCase());
            }
            for (String candidate : candidates) {
                if (!familySet.contains(candidate.toLowerCase())) {
                    continue;
                }
                Font testFont = new Font(candidate, Font.PLAIN, 18);
                if (testFont.canDisplayUpTo(probe) == -1) {
                    resolved.add(candidate);
                }
            }
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("captcha path fonts={}, fallbackSvg={}",
                        resolved, resolved.isEmpty());
            }
        } catch (Exception ignore) {
        }
        return resolved;
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
            if (pointInput == null) {
                return false;
            }
            if (pointInput.x() < 0 || pointInput.x() > 1 || pointInput.y() < 0 || pointInput.y() > 1) {
                return false;
            }
            int px = (int) Math.round(pointInput.x() * challenge.width());
            int py = (int) Math.round(pointInput.y() * challenge.height());
            // 点在提示区直接失败
            if (py < clickTop) {
                return false;
            }
            CaptchaRecord.Glyph targetGlyph = findTargetGlyphByOrder(challenge.glyphList(), i);
            if (targetGlyph == null || !isHit(px, py, targetGlyph, tolerance)) {
                return false;
            }
        }
        return true;
    }

    private static CaptchaRecord.Glyph findTargetGlyphByOrder(List<CaptchaRecord.Glyph> glyphList, int order) {
        if (Arr.isEmpty(glyphList)) {
            return null;
        }
        for (CaptchaRecord.Glyph glyph : glyphList) {
            if (glyph.target() && glyph.targetOrder() == order) {
                return glyph;
            }
        }
        return null;
    }

    private static boolean isHit(int px, int py, CaptchaRecord.Glyph glyph, int tolerance) {
        // 以字形半径为主, 仅加少量容差, 避免旧逻辑把命中圈扩到整图高度
        int radius = glyph.radius() + Math.min(4, tolerance / 3);
        if (radius < tolerance) {
            radius = tolerance;
        }
        int dx = glyph.x() - px;
        int dy = glyph.y() - py;
        return dx * dx + dy * dy <= radius * radius;
    }


    private record CaptchaTheme(
            boolean dark,
            String bgColor,
            String primaryColor,
            String secondaryColor,
            String dividerColor
    ) {
    }

    private record SvgPath(String d, String fill, float strokeWidth, String stroke) {
    }

    private record GlyphBuild(CaptchaRecord.Glyph glyph, List<SvgPath> paths) {
    }
}
