package com.github.global.constant;

public final class RedisConst {

    public static String exampleKey(String action, String param) {
        return String.format("%s-%s:%s", action, "example", param);
    }
}
