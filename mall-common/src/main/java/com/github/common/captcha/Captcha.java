package com.github.common.captcha;

import com.github.common.util.Arr;
import com.github.common.util.LogUtil;
import com.github.common.util.Obj;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 点选验证码公共能力: 字库/字体/布局/模型/校验.
 */
public class Captcha {

    public static final int TARGET_COUNT_MIN = 3;
    public static final int TARGET_COUNT_MAX = 4;

    /** 图默认尺寸与提示区高度(与前端兜底一致) */
    public static final int DEFAULT_CAPTCHA_WIDTH = 224;
    public static final int DEFAULT_CAPTCHA_HEIGHT = 88;
    public static final int PROMPT_AREA_HEIGHT = 32;

    static final int WIDTH_MIN = 200;
    static final int WIDTH_MAX = 400;
    static final int HEIGHT_MIN = 72;
    static final int HEIGHT_MAX = 140;
    static final int NOISE_COUNT_MIN = 2;
    static final int GLYPH_TOTAL_MAX = 7;

    static final int DEFAULT_CLICK_TOLERANCE_PX = 12;

    /**
     * 同类安全字池.
     */
    static final String[] SAFE_CHAR_GROUPS = new String[] {
            // 天干地支
            "甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥",
            // 季节方位时空
            "春夏秋冬东南西北中上下左右前后内外远高低晨昏昼夜早晚寒暖干湿明暗深浅长短宽窄方圆斜正偏岁月时刻分秒",
            // 动物(不含易辱骂: 鸡狗猪蛇鸟鼠驴骡龟狼狐猴鸭熊鸦)
            "猫兔牛羊马鹅鱼虾蟹虎豹狮象鹿蜂蝶蚁燕雀鹰蛙鲸豚鲨驼鹤鸥鸽鹃莺蜗蛛蝠狸貂雁猩犀鳄蚕萤蝉蜻蜓鲤鲫鲈鲑鳟鲶鳗蚌蛤螺麋麂獐獭鹦鹉鹂鹌鹬螳螂蟋蟀蚯蚓鹭鹏鲲鸾凤",
            // 植物蔬果
            "松柏柳杨桐樟楠梧桃杏梅兰竹花树稻麦豆枫桦杉荷莲藕梨枣橙柚柠瓜果葱姜蒜萝卜芹薯芋笋藤蔓枝叶芽蕾苔蕨槐榆梓椿榕樱芙蓉牡丹芍药葵芝菇耳蕉芒荔柿苹葡萄椰茉莉桂丁香蔷薇海棠橄榄栗莓菠菜芥番茄椒薏",
            // 自然天象地貌
            "山水云风雨雪月星河湖海川泉雷电霜冰露雾霞虹沙石泥岛滩峰谷坡溪潭瀑岭崖洲湾泊汐岩原野田林森漠雹霖涧峡丘麓岚霭霓堤堰坝焰霏渚塬",
            // 颜色
            "红黄蓝青绿紫黑白灰金银铜橙粉棕墨素苍碧翠黛褐绯绛茜靛玄",
            // 器物用具
            "桌椅凳碗筷勺笔墨纸琴棋书杯盘碟锅铲盆针线布灯烛门窗墙桥塔亭船车轮钟鼓铃镜盒箱袋伞扇壶瓶罐匣柜毯帘幕梯栏锁钥尺秤鼎炉灶筛磨斧锯锤钉钩绳网篮筐箕帚耙犁锄锹镐镰砚印箫笛笙筝瑟旗帆桨舵鞍缰桶缸瓮梳篦簪瓦砖梁柱椽",
            // 食材饮品
            "米面油盐醋茶酒汤糖粥饼糕酱蜜酪酥饺馒饭菜肴羹汁浆豉椒茴饴蔬腐醴",
            // 学习建筑
            "读写算练卷册页章楼阁厅廊坊苑殿堂室"
    };

    /** 数字白名单 */
    static final String[] NUMBER_TRIPLETS = new String[] {
            "零一二", "一二三", "二三五", "一三五", "三五七", "五七十",
            "七十百", "十百千", "百千万", "二三七", "一三七", "一五十",
            "二五十", "三七十", "零三五", "零五七",
            "零壹贰", "壹贰叁", "贰叁伍", "壹叁伍", "叁伍柒", "伍柒拾",
            "拾佰仟", "佰仟萬", "仟萬億", "個拾佰", "贰伍柒", "零叁伍", "壹伍柒"
    };

    /** 干扰字池 */
    static final String SAFE_NOISE_POOL;
    static {
        String s = """
                甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥
                春夏秋冬东南西北中上下左右前后内外远高低晨昏昼夜早晚寒暖干湿明暗深浅长短宽窄方圆斜正偏岁月时刻分秒
                猫兔牛羊马鹅鱼虾蟹虎豹狮象鹿蜂蝶蚁燕雀鹰蛙鲸豚鲨驼鹤鸥鸽鹃莺蜗蛛蝠狸貂雁猩犀鳄蚕萤蝉蜻蜓鲤鲫鲈鲑鳟鲶鳗蚌蛤螺麋麂獐獭鹦鹉鹂鹌鹬螳螂蟋蟀蚯蚓鹭鹏鲲鸾凤
                松柏柳杨桐樟楠梧桃杏梅兰竹花树稻麦豆枫桦杉荷莲藕梨枣橙柚柠瓜果葱姜蒜萝卜芹薯芋笋藤蔓枝叶芽蕾苔蕨槐榆梓椿榕樱芙蓉牡丹芍药葵芝菇耳蕉芒荔柿苹葡萄椰茉莉桂丁香蔷薇海棠橄榄栗莓菠菜芥番茄椒薏
                山水云风雨雪月星河湖海川泉雷电霜冰露雾霞虹沙石泥岛滩峰谷坡溪潭瀑岭崖洲湾泊汐岩原野田林森漠雹霖涧峡丘麓岚霭霓堤堰坝焰霏渚塬
                红黄蓝青绿紫黑白灰金银铜铁橙粉棕墨素苍碧翠黛褐绯绛茜靛玄
                桌椅凳碗筷勺笔墨纸琴棋书杯盘碟锅铲盆针线布灯烛门窗墙桥塔亭船车轮钟鼓铃镜盒箱袋伞扇壶瓶罐匣柜毯帘幕梯栏锁钥尺秤鼎炉灶筛磨斧锯锤钉钩绳网篮筐箕帚耙犁锄锹镐镰砚印箫笛笙筝瑟旗帆桨舵鞍缰桶缸瓮梳篦簪瓦砖梁柱椽
                米面油盐醋茶酒汤糖粥饼糕酱蜜酪酥饺馒饭菜肴羹汁浆豉椒茴饴蔬腐醴
                读写算练卷册页章楼阁厅廊坊苑殿堂室
                零一二三五七十百千万
                壹贰叁伍柒個拾佰仟萬億
                """;
        Set<String> set = new LinkedHashSet<>();
        for (char c : s.toCharArray()) {
            String v = String.valueOf(c).trim();
            if (!v.isEmpty()) {
                set.add(v);
            }
        }
        SAFE_NOISE_POOL = String.join("", set);
    }

    /**
     * 本机可真实显示中文的字体族(不含 Dialog/DejaVu/Nimbus 等伪支持).
     * 最小安装(装完需重启 JVM): yum install -y wqy-microhei-fonts / apt install -y fonts-wqy-microhei
     */
    static final List<String> GLYPH_FONT_FAMILIES = resolveGlyphFontFamilies();

    private static final ConcurrentHashMap<String, Font> FONT_CACHE = new ConcurrentHashMap<>();

    /** 按本机中文字体裁剪后的干扰池 / 同类字池 / 数字白名单 */
    static final String RENDERABLE_NOISE_POOL;
    static final List<String> RENDERABLE_GROUPS;
    static final List<String> RENDERABLE_NUMBER_TRIPLETS;
    static {
        Font font = pickFont(18, false);
        RENDERABLE_NOISE_POOL = filterNoisePool(font);
        RENDERABLE_GROUPS = filterGroups(font);
        RENDERABLE_NUMBER_TRIPLETS = filterNumberTriplets(font);
    }

    /** 本机是否有可用的中文字体  */
    public static boolean hasCjkFonts() {
        return Arr.isNotEmpty(GLYPH_FONT_FAMILIES);
    }

    /** 按字号取 Font(带缓存). preferAlternate 时尽量换用非首选族. */
    static Font pickFont(int size, boolean preferAlternate) {
        String family;
        if (hasCjkFonts()) {
            int idx = 0;
            if (preferAlternate && GLYPH_FONT_FAMILIES.size() > 1) {
                idx = 1 + Obj.RANDOM.nextInt(GLYPH_FONT_FAMILIES.size() - 1);
            } else if (GLYPH_FONT_FAMILIES.size() > 1 && Obj.RANDOM.nextBoolean()) {
                idx = Obj.RANDOM.nextInt(GLYPH_FONT_FAMILIES.size());
            }
            family = GLYPH_FONT_FAMILIES.get(idx);
        } else {
            family = Font.DIALOG;
        }
        String key = family + "|" + size;
        //noinspection unused
        return FONT_CACHE.computeIfAbsent(key, k -> new Font(family, Font.PLAIN, size));
    }

    public static List<String> pickTargets(int targetCount) {
        return pickTargets(targetCount, Arrays.asList(SAFE_CHAR_GROUPS), Arrays.asList(NUMBER_TRIPLETS));
    }

    public static List<String> pickTargets(int targetCount, List<String> groups, List<String> numberTriplets) {
        boolean numberUsable = targetCount == 3 && groups != null && !numberTriplets.isEmpty();
        List<String> usableGroups = groups == null ? List.of() : groups;
        int sourceCount = usableGroups.size() + (numberUsable ? 1 : 0);
        if (sourceCount == 0) {
            return fallbackTargets(targetCount);
        }
        Random random = Obj.RANDOM;
        int idx = random.nextInt(sourceCount);
        if (idx < usableGroups.size()) {
            List<String> list = pickDistinctChars(usableGroups.get(idx), targetCount);
            return list.size() == targetCount ? list : fallbackTargets(targetCount);
        }
        String raw = numberTriplets.get(random.nextInt(numberTriplets.size()));
        List<String> list = new ArrayList<>(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            list.add(String.valueOf(raw.charAt(i)));
        }
        Collections.shuffle(list, random);
        return list;
    }

    public static List<String> pickNoise(List<String> exclude, int noiseCount) {
        return pickNoise(exclude, noiseCount, SAFE_NOISE_POOL);
    }

    /** Path/Png 用: 按本机字体裁剪后的池抽目标字 */
    static List<String> pickRenderableTargets(int targetCount) {
        return pickTargets(targetCount, RENDERABLE_GROUPS, RENDERABLE_NUMBER_TRIPLETS);
    }

    /** Path/Png 用: 按本机字体裁剪后的池抽干扰字 */
    static List<String> pickRenderableNoise(List<String> exclude, int noiseCount) {
        return pickNoise(exclude, noiseCount, RENDERABLE_NOISE_POOL);
    }

    static int resolveWidth(String width) {
        return resolveSize(width, DEFAULT_CAPTCHA_WIDTH, WIDTH_MIN, WIDTH_MAX);
    }

    static int resolveHeight(String height) {
        return resolveSize(height, DEFAULT_CAPTCHA_HEIGHT, HEIGHT_MIN, HEIGHT_MAX);
    }

    static int promptBottom(int imageHeight) {
        return Math.min(PROMPT_AREA_HEIGHT, Math.max(24, imageHeight - 40));
    }

    static int randomTargetCount() {
        return TARGET_COUNT_MIN + Obj.RANDOM.nextInt(TARGET_COUNT_MAX - TARGET_COUNT_MIN + 1);
    }

    static int randomNoiseCount(int targetCount) {
        return NOISE_COUNT_MIN + Obj.RANDOM.nextInt(GLYPH_TOTAL_MAX - targetCount - NOISE_COUNT_MIN + 1);
    }

    /** 解析 URL 宽高: 未传或非法用默认, 否则夹在 min~max */
    static int resolveSize(String param, int defaultPx, int minPx, int maxPx) {
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
     * 每个字独占一段水平区间并抖动, 减少叠字.
     */
    static List<Point> randomPoints(int width, int height, int fontSize, int count) {
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
        List<Point> pointList = new ArrayList<>(count);
        if (innerW < 1 || innerH < 1 || count == 0) {
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
            pointList.add(new Point(x, y));
        }
        return pointList;
    }

    public static List<String> pickNoise(List<String> exclude, int noiseCount, String noisePool) {
        List<String> list = new ArrayList<>();
        if (Obj.isBlank(noisePool) || noiseCount <= 0) {
            return list;
        }
        int total = noisePool.length();
        int guard = 0;
        Random random = Obj.RANDOM;
        while (list.size() < noiseCount && guard < total * 8) {
            guard++;
            String value = String.valueOf(noisePool.charAt(random.nextInt(total)));
            if (list.contains(value) || (exclude != null && exclude.contains(value))) {
                continue;
            }
            list.add(value);
        }
        return list;
    }

    /** 按字体可显示能力裁剪干扰池 */
    public static String filterNoisePool(Font font) {
        if (font == null) {
            return SAFE_NOISE_POOL;
        }
        StringBuilder builder = new StringBuilder(SAFE_NOISE_POOL.length());
        for (int i = 0; i < SAFE_NOISE_POOL.length(); i++) {
            char c = SAFE_NOISE_POOL.charAt(i);
            if (font.canDisplay(c)) {
                builder.append(c);
            }
        }
        return !builder.isEmpty() ? builder.toString() : SAFE_NOISE_POOL;
    }

    public static List<String> filterGroups(Font font) {
        List<String> list = new ArrayList<>();
        for (String group : SAFE_CHAR_GROUPS) {
            StringBuilder builder = new StringBuilder(group.length());
            for (int i = 0; i < group.length(); i++) {
                char c = group.charAt(i);
                if (font == null || font.canDisplay(c)) {
                    builder.append(c);
                }
            }
            if (builder.length() >= Math.max(5, TARGET_COUNT_MAX + 1)) {
                list.add(builder.toString());
            }
        }
        if (list.isEmpty()) {
            list.addAll(Arrays.asList(SAFE_CHAR_GROUPS));
        }
        return list;
    }

    public static List<String> filterNumberTriplets(Font font) {
        List<String> list = new ArrayList<>();
        for (String triplet : NUMBER_TRIPLETS) {
            if (font == null || font.canDisplayUpTo(triplet) == -1) {
                list.add(triplet);
            }
        }
        return list;
    }

    private static List<String> fallbackTargets(int targetCount) {
        return pickDistinctChars("春夏秋冬东南西北中", targetCount);
    }

    private static List<String> pickDistinctChars(String pool, int count) {
        List<String> list = new ArrayList<>(count);
        int total = pool.length();
        int guard = 0;
        while (list.size() < count && guard++ < total * 6) {
            String value = String.valueOf(pool.charAt(Obj.RANDOM.nextInt(total)));
            if (!list.contains(value)) {
                list.add(value);
            }
        }
        return list;
    }

    private static List<String> resolveGlyphFontFamilies() {
        List<String> resolved = new ArrayList<>();
        final String probe = "验证码春夏秋";
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
                LogUtil.ROOT_LOG.info("captcha cjk fonts={}, empty={}", resolved, resolved.isEmpty());
            }
        } catch (Exception ignore) {
        }
        return List.copyOf(resolved);
    }


    /** 图内坐标点(像素) */
    public static final class Point {
        private final int x;
        private final int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }
    }

    /** 单个字形元数据(点击命中用) */
    public static final class Glyph {
        private final String value;
        private final int x;
        private final int y;
        private final int radius;
        private final boolean target;
        private final int targetOrder;

        public Glyph(String value, int x, int y, int radius, boolean target, int targetOrder) {
            this.value = value;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.target = target;
            this.targetOrder = targetOrder;
        }

        public String value() {
            return value;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int radius() {
            return radius;
        }

        public boolean target() {
            return target;
        }

        public int targetOrder() {
            return targetOrder;
        }
    }

    /** 挑战(服务端缓存, 校验点击用) */
    public static final class Challenge {
        private final List<String> targetChars;
        private final List<Glyph> glyphList;
        private final int width;
        private final int height;
        private final int clickAreaTop;

        public Challenge(List<String> targetChars, List<Glyph> glyphList, int width, int height, int clickAreaTop) {
            this.targetChars = targetChars;
            this.glyphList = glyphList;
            this.width = width;
            this.height = height;
            this.clickAreaTop = clickAreaTop;
        }

        public List<String> targetChars() {
            return targetChars;
        }

        public List<Glyph> glyphList() {
            return glyphList;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public int clickAreaTop() {
            return clickAreaTop;
        }
    }

    /** 生成结果(图 data URI + 挑战) */
    public static final class Build {
        private final String image;
        private final int width;
        private final int height;
        private final Challenge challenge;

        public Build(String image, int width, int height, Challenge challenge) {
            this.image = image;
            this.width = width;
            this.height = height;
            this.challenge = challenge;
        }

        public String image() {
            return image;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public Challenge challenge() {
            return challenge;
        }
    }

    /** 前端点击点(相对坐标 0~1) */
    public static final class PointInput {
        private final double x;
        private final double y;

        public PointInput(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }
    }

    /** 三种生成格式共用的点击校验 */
    public static boolean verifyClick(Challenge challenge, List<PointInput> points, Integer tolerancePx) {
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
            PointInput pointInput = points.get(i);
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
            Glyph targetGlyph = findTargetGlyphByOrder(challenge.glyphList(), i);
            if (targetGlyph == null || !isHit(px, py, targetGlyph, tolerance)) {
                return false;
            }
        }
        return true;
    }

    private static Glyph findTargetGlyphByOrder(List<Glyph> glyphList, int order) {
        if (Arr.isEmpty(glyphList)) {
            return null;
        }
        for (Glyph glyph : glyphList) {
            if (glyph.target() && glyph.targetOrder() == order) {
                return glyph;
            }
        }
        return null;
    }

    private static boolean isHit(int px, int py, Glyph glyph, int tolerance) {
        // 以字形半径为主, 仅加少量容差
        int radius = glyph.radius() + Math.min(4, tolerance / 3);
        if (radius < tolerance) {
            radius = tolerance;
        }
        int dx = glyph.x() - px;
        int dy = glyph.y() - py;
        return dx * dx + dy * dy <= radius * radius;
    }
}
