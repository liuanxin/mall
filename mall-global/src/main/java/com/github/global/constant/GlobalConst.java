package com.github.global.constant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class GlobalConst {

    public static final String MODULE_NAME = "global";

    /** web 层入参、出参日志打印排除 url 清单 */
    public static final Set<String> EXCLUDE_PATH_SET = new HashSet<>(Arrays.asList(
            "/actuator/health",
            "/actuator/prometheus"
    ));

    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TWO = 2;
    public static final int THREE = 3;
    public static final int FOUR = 4;
    public static final int FIVE = 5;
    public static final int SIX = 6;
    public static final int SEVEN = 7;
    public static final int EIGHT = 8;
    public static final int NINE = 9;
    public static final int TEN = 10;
}
