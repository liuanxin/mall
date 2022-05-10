package com.github.common.util;

import com.github.common.Const;

import java.util.Arrays;
import java.util.List;

/** 脱敏工具类 */
public final class DesensitizationUtil {

    private static final List<String> SENSITIVE_LIST = Arrays.asList("*", "**", "****", "*****");

    public static String des(String key, String value) {
        if (U.isBlank(key) || U.isBlank(value)) {
            return SENSITIVE_LIST.get(U.RANDOM.nextInt(SENSITIVE_LIST.size()));
        }

        String lower = key.toLowerCase();
        if (lower.equals(Const.TOKEN.toLowerCase())) {
            return U.foggyToken(value);
        }

        switch (lower) {
            case "password": {
                return "***";
            }
            case "phone": {
                return U.foggyPhone(value);
            }
            case "id-card":
            case "idcard":
            case "id_card": {
                return U.foggyIdCard(value);
            }
            default: {
                return U.foggyValue(value, 300, 100);
            }
        }
    }
}
