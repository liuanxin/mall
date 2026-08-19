package com.github.common.captcha;

import com.github.common.util.Obj;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * 验证码抽字与字体裁剪.
 */
class CaptchaChars {

    public static final int TARGET_COUNT_MIN = 3;
    public static final int TARGET_COUNT_MAX = 4;

    private CaptchaChars() {
    }

    public static List<String> pickTargets(int targetCount) {
        return pickTargets(targetCount, Arrays.asList(CaptchaLexicon.SAFE_CHAR_GROUPS),
                Arrays.asList(CaptchaLexicon.NUMBER_TRIPLETS));
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
        return pickNoise(exclude, noiseCount, CaptchaLexicon.SAFE_NOISE_POOL);
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
            return CaptchaLexicon.SAFE_NOISE_POOL;
        }
        StringBuilder builder = new StringBuilder(CaptchaLexicon.SAFE_NOISE_POOL.length());
        for (int i = 0; i < CaptchaLexicon.SAFE_NOISE_POOL.length(); i++) {
            char c = CaptchaLexicon.SAFE_NOISE_POOL.charAt(i);
            if (font.canDisplay(c)) {
                builder.append(c);
            }
        }
        return !builder.isEmpty() ? builder.toString() : CaptchaLexicon.SAFE_NOISE_POOL;
    }

    public static List<String> filterGroups(Font font) {
        List<String> list = new ArrayList<>();
        for (String group : CaptchaLexicon.SAFE_CHAR_GROUPS) {
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
            list.addAll(Arrays.asList(CaptchaLexicon.SAFE_CHAR_GROUPS));
        }
        return list;
    }

    public static List<String> filterNumberTriplets(Font font) {
        List<String> list = new ArrayList<>();
        for (String triplet : CaptchaLexicon.NUMBER_TRIPLETS) {
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
}
