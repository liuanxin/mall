package com.github.global.constant;

import com.google.common.collect.Sets;

import java.util.Set;

public final class GlobalConst {

    public static final String MODULE_NAME = "global";

    /** web 层入参、出参日志打印排除 url 清单 */
    public static final Set<String> EXCLUDE_PATH_SET = Sets.newHashSet(
            "/actuator/health",
            "/actuator/prometheus"
    );
}
