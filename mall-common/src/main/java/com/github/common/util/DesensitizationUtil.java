package com.github.common.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/** 脱敏工具类 */
public final class DesensitizationUtil {

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    /** 基于 key 脱敏 */
    public static String desWithKey(String key, String value) {
        if (U.isBlank(key) || U.isBlank(value)) {
            return value;
        }

        String lower = key.toLowerCase();
        String str = lower.contains("_") ? lower.replace("_", "-") : lower;
        return switch (str) {
            case "username", "user-name" -> U.foggyValue(value, 2, 1, 0, true);
            case "pwd", "password" -> "***";
            case "phone", "tel", "telephone" -> U.foggyValue(value, 11, 3, 4, false);
            case "address", "street" -> U.foggyValue(value, 10, 3);
            case "idcard", "id-card" -> U.foggyValue(value, 15, 2);
            case "authorization", "x-auth-key",
                    "token", "x-token", "apptoken", "app-token", "x-app-token",
                    "appkey", "app-key", "x-app-key",
                    "appsecret", "app-secret", "x-app-secret" -> U.foggyValue(value, 16, 3);
            default -> U.foggyValue(value, 500, 100);
        };
    }

    /** 字符串脱敏, 将开始索引和结束索引之间的字符替换成「***」 */
    public static String desString(String value, int startIndex, int endIndex) {
        int start = Math.max(0, startIndex);
        int end = Math.max(0, endIndex);
        int length = value.length();

        StringBuilder sbd = new StringBuilder();
        if (start < length) {
            sbd.append(value, 0, start);
        }
        sbd.append("***");
        if (end > 0 && length > (start + end + 3)) {
            sbd.append(value, length - end, length);
        }
        return sbd.toString().trim();
    }

    /** 数字脱敏, 浮点数(BigDecimal 或 double 或 float)随机且指定小数位并输出成字符串, 大数(bitInteger 或 long)随机并输出成字符串, 小数随机 */
    public static Object desNumber(Number value, double randomNumber, int digitsNumber) {
        double random = Math.abs(randomNumber);
        if (value instanceof BigDecimal || value instanceof Double || value instanceof Float) {
            double d = (random != 0) ? RANDOM.nextDouble(random) : value.doubleValue();
            // BigDecimal 或 float 或 double 使用 String 序列化    去掉尾部的 0(比如 10.00 --> 10    10.020 -> 10.02)
            return BigDecimal.valueOf(d).setScale(Math.abs(digitsNumber), RoundingMode.DOWN).stripTrailingZeros().toPlainString();
        }
        else if (value instanceof BigInteger || value instanceof Long) {
            // long 或 BigInt 使用 String 序列化
            return Long.toString((random != 0) ?  RANDOM.nextLong((long) random) : value.longValue());
        }
        else if (value instanceof Integer || value instanceof Short) {
            return (random != 0) ? RANDOM.nextInt((int) random) : value.intValue();
        }
        else {
            return value;
        }
    }

    /** 时间脱敏, 返回比指定时间大或者小的时间戳, 随机范围在 1 ~ 指定值 之间, 大还是小也是随机 */
    public static long desDate(Date value, long randomDateTimeMillis) {
        long ms = value.getTime();
        long random = Math.abs(randomDateTimeMillis);
        if (random != 0) {
            long r = RANDOM.nextLong(1L, random);
            return Math.abs(RANDOM.nextBoolean() ? (ms + r) : (ms - r));
        } else {
            return ms;
        }
    }
}
