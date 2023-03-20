package com.github.common.util;

import com.github.common.date.DateUtil;
import com.github.common.exception.*;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** 工具类 */
public final class U {

    public static final Random RANDOM = new Random();

    /** 本机的 cpu 核心数 */
    public static final int CPU_SIZE = Runtime.getRuntime().availableProcessors();

    public static final String EMPTY = "";

    /** 递归时的最大深度, 避免无限递归 */
    public static final int MAX_DEPTH = 20;

    /** 手机号. 见 <a href="https://zh.wikipedia.org/wiki/%E4%B8%AD%E5%9B%BD%E5%86%85%E5%9C%B0%E7%A7%BB%E5%8A%A8%E7%BB%88%E7%AB%AF%E9%80%9A%E8%AE%AF%E5%8F%B7%E6%AE%B5">https://zh.wikipedia.org/wiki/%E4%B8%AD%E5%9B%BD%E5%86%85%E5%9C%B0%E7%A7%BB%E5%8A%A8%E7%BB%88%E7%AB%AF%E9%80%9A%E8%AE%AF%E5%8F%B7%E6%AE%B5</a> */
    private static final String PHONE = "^1[3-9]\\d{9}$";
    /** _abc-def@123-hij.uvw_xyz.com 是正确的, -123@xyz.com 不是 */
    private static final String EMAIL = "^\\w[\\w\\-]*@([\\w\\-]+\\.\\w+)+$";
    /** ico, jpeg, jpg, bmp, png, svg 后缀 */
    private static final String IMAGE = "(?i)^(.*)\\.(ico|jpeg|jpg|bmp|png|svg)$";
    /** IPv4 地址 */
    private static final String IPV4 = "^([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])(\\.([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])){3}$";
    /** 身份证号码 */
    private static final String ID_CARD = "(^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$)|(^[1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}$)";

    /** 中文 */
    private static final String CHINESE = "[\\u4e00-\\u9fa5]";
    /** 是否是移动端. 见 <a href="https://gist.github.com/dalethedeveloper/1503252">https://gist.github.com/dalethedeveloper/1503252</a> */
    private static final String MOBILE = "(?i)Mobile|iP(hone|od|ad)|Android|BlackBerry|Blazer|PSP|UCWEB|IEMobile|Kindle|NetFront|Silk-Accelerated|(hpw|web)OS|Fennec|Minimo|Opera M(obi|ini)|Dol(f|ph)in|Skyfire|Zune";
    /** 是否是 pc 端 */
    private static final String PC = "(?i)AppleWebKit|Mozilla|Chrome|Safari|MSIE|Windows NT";
    /** 是否是本地 ip */
    private static final String LOCAL = "(?i)127.0.0.1|localhost|::1|0:0:0:0:0:0:0:1";

    /**
     * <pre>
     * 字符串长度 >= 这个值, 才进行压缩
     *   字符串压缩时, 将字符串先操作 gzip 再编码成 base64 字符串
     *   解压字符串时, 将字符串先解码 base64 再操作 gzip 解压
     * </pre>
     */
    private static final int COMPRESS_MIN_LEN = 1000;

    private static final String TMP = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final Pattern THOUSANDS_REGEX = Pattern.compile("(\\d)(?=(?:\\d{3})+$)");

    public static final Set<String> TRUES = new HashSet<>(4, 1);
    static {
        TRUES.add("true");
        TRUES.add("1");
        TRUES.add("on");
        TRUES.add("yes");
    }

    private static final Map<String, Map<String, Method>> METHODS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Field>> FIELDS_CACHE = new ConcurrentHashMap<>();


    /**
     * <pre>
     * 《Java Concurrency in Practice》即《Java 并发编程实战》
     *     Ncpu = CPU 可用核心数
     *     Ucpu = CPU 的使用率(0 <= Ucpu <= 1)
     *     W/C  = 等待时间与计算时间的比率
     *     最优的池的大小等于: Nthreads = Ncpu x Ucpu x (1 + W/C)
     *
     * 《Programming Concurrency on the JVM》即《Java 虚拟机并发编程》
     *     线程数 = CPU 可用核心数 / (1 - 阻塞系数), 其中阻塞系数的取值大于 0 且小于 1
     *     计算密集型任务的阻塞系数接近 0, 而 IO 密集型任务的阻塞系数则接近 1
     *
     *
     * 纯计算型: CPU 密集型
     *     配置尽可能小的线程数, 如配置 CPU 数 + 1 个线程(当发生未知原因而暂停时, 刚好有 1 个"额外"的线程可以确保在这种情况下 CPU 周期不会中断工作)
     *
     * 现实的情况通常是: 少量 CPU 密集型 + 大量 IO 密集型(网络, 磁盘)
     *     如果一个任务 CPU 耗时 10ms, IO 耗时 190ms, 则单个 CPU 的利用率是 10 / (10 + 190),
     *     想要每个 CPU 的使用率达到 90%, 且有 4 个 CPU 核心, 最终: 4 * 0.9 * (10 + 190) / 10 = 72
     *
     *
     * CPU 利用率  0.7, CPU 时间:   10, IO 时间:  190 ==> 56
     * CPU 利用率  0.7, CPU 时间:   20, IO 时间:  180 ==> 28
     * CPU 利用率  0.7, CPU 时间:  100, IO 时间:    1 ==>  3
     * CPU 利用率  0.7, CPU 时间: 1000, IO 时间:    1 ==>  3
     *
     * CPU 利用率 0.75, CPU 时间:   10, IO 时间:  190 ==> 60
     * CPU 利用率 0.75, CPU 时间:   20, IO 时间:  180 ==> 30
     * CPU 利用率 0.75, CPU 时间:  100, IO 时间:    1 ==>  5
     * CPU 利用率 0.75, CPU 时间: 1000, IO 时间:    1 ==>  5
     *
     * CPU 利用率  0.8, CPU 时间:   10, IO 时间:  190 ==> 64
     * CPU 利用率  0.8, CPU 时间:   20, IO 时间:  180 ==> 32
     * CPU 利用率  0.8, CPU 时间:  100, IO 时间:    1 ==>  5
     * CPU 利用率  0.8, CPU 时间: 1000, IO 时间:    1 ==>  5
     *
     * CPU 利用率 0.85, CPU 时间:   10, IO 时间:  190 ==> 68
     * CPU 利用率 0.85, CPU 时间:   20, IO 时间:  180 ==> 34
     * CPU 利用率 0.85, CPU 时间:  100, IO 时间:    1 ==>  5
     * CPU 利用率 0.85, CPU 时间: 1000, IO 时间:    1 ==>  5
     * </pre>
     *
     * @param cpuRate cpu 占用率, 0 ~ 1 之间的小数
     * @param cpuTime 单个任务中 CPU 占用的毫秒数(1 ~ 999 之间), 当 CPU 密集型时这个值很大(接近 1000), 当 IO 密集型时这个值很小(接近 0)
     * @param ioTime 单个任务中 IO 占用的毫秒数(1 ~ 999 之间), 当 CPU 密集型时这个值很小(接近 0), 当 IO 密集型时这个值很大(接近 1000)
     */
    public static int calcPoolSize(double cpuRate, int cpuTime, int ioTime) {
        double realRate = (cpuRate > 0 && cpuRate < 1) ? cpuRate : 1;
        int realCpuTime = (cpuTime <= 0) ? 1 : Math.min(cpuTime, 999);
        int realIoTime = (ioTime <= 0) ? 1 : Math.min(ioTime, 999);
        double num = CPU_SIZE * realRate * (realCpuTime + realIoTime) / realCpuTime;
        int calcNum = (int) num;
        int returnNum = (num == calcNum) ? calcNum : (calcNum + 1);
        return (returnNum == CPU_SIZE) ? (returnNum + 1) : returnNum;
    }

    /** 生成指定位数的随机数: 纯数字 */
    public static String random(int length) {
        if (length <= 0) {
            return EMPTY;
        }

        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sbd.append(RANDOM.nextInt(10));
        }
        return sbd.toString();
    }
    /** 生成指定位数的随机数: 数字和字母 */
    public static String randomLetterAndNumber(int length) {
        if (length <= 0) {
            return EMPTY;
        }

        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sbd.append(TMP.charAt(RANDOM.nextInt(TMP.length())));
        }
        return sbd.toString();
    }


    /**
     * 获取枚举中的值, 先匹配 name, 再匹配 getCode 方法, 再匹配 getValue 方法, 都匹配不上则返回 null
     *
     * @param clazz 枚举的类信息
     * @param obj 要匹配的值
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Enum> E toEnum(Class<E> clazz, Object obj) {
        if (isNotNull(obj)) {
            E[] constants = clazz.getEnumConstants();
            if (constants != null && constants.length > 0) {
                String source = obj.toString().trim();
                for (E em : constants) {
                    // 如果传递过来的是枚举名, 且能匹配上则返回
                    if (source.equalsIgnoreCase(em.name())) {
                        return em;
                    }

                    // 如果传递过来的值跟枚举的 getCode(数字) 相同则返回
                    Object code = invokeMethod(em, "getCode");
                    if (isNotNull(code) && source.equalsIgnoreCase(code.toString().trim())) {
                        return em;
                    }

                    // 如果传递过来的值跟枚举的 getValue(中文) 相同则返回
                    Object value = invokeMethod(em, "getValue");
                    if (isNotNull(value) && source.equalsIgnoreCase(value.toString().trim())) {
                        return em;
                    }

                    // 如果传递过来的值跟枚举的 ordinal(数字. 表示枚举所在的索引) 相同则返回
                    // if (source.equalsIgnoreCase(toStr(em.ordinal()))) return em;
                }
            }
        }
        return null;
    }


    /** 传入的数不为 null 且 大于 0 就返回 true */
    public static boolean greater0(Number obj) {
        return obj != null && obj.doubleValue() > 0;
    }
    /** 传入的数为 null 或 小于等于 0 就返回 true */
    public static boolean lessAndEquals0(Number obj) {
        return !greater0(obj);
    }

    /** 传入的数不为 null 且 大于等于 0 就返回 true */
    public static boolean greaterAndEquals0(Number obj) {
        return obj != null && obj.doubleValue() >= 0;
    }
    /** 传入的数为 null 或 小于 0 就返回 true(等于 0 时返回 false) */
    public static boolean less0(Number obj) {
        return !greaterAndEquals0(obj);
    }

    /** 转换成 int, 非数字则返回 0 */
    public static int toInt(Object obj) {
        return toInt(obj, 0);
    }
    /** 转换成 int, 非数字则返回默认值 */
    public static int toInt(Object obj, int defaultInt) {
        if (isNull(obj)) {
            return defaultInt;
        }
        if (obj instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return defaultInt;
        }
    }

    /** 转换成 long, 非数字则返回 0L */
    public static long toLong(Object obj) {
        return toLong(obj, 0L);
    }
    /** 转换成 long, 非数字则返回默认值 */
    public static long toLong(Object obj, long defaultLong) {
        if (isNull(obj)) {
            return defaultLong;
        }
        if (obj instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(obj.toString().trim());
        } catch (NumberFormatException e) {
            return defaultLong;
        }
    }

    /** 转换成 float, 非数字则返回 0F */
    public static float toFloat(Object obj) {
        return toFloat(obj, 0F);
    }
    /** 转换成 float, 非数字则返回默认值 */
    public static float toFloat(Object obj, float defaultFloat) {
        if (isNull(obj)) {
            return defaultFloat;
        }
        if (obj instanceof Number n) {
            return n.floatValue();
        }
        try {
            return Float.parseFloat(obj.toString().trim());
        } catch (NumberFormatException e) {
            return defaultFloat;
        }
    }

    /** 转换成 double, 非数字则返回 0D */
    public static double toDouble(Object obj) {
        return toDouble(obj, 0D);
    }
    /** 转换成 double, 非数字则返回 0D */
    public static double toDouble(Object obj, double defaultDouble) {
        if (isNull(obj)) {
            return defaultDouble;
        }
        if (obj instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString().trim());
        } catch (NumberFormatException e) {
            return defaultDouble;
        }
    }

    public static boolean isInt(Object obj) {
        if (isNull(obj)) {
            return false;
        }
        try {
            Integer.parseInt(obj.toString().trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public static boolean isNotInt(Object obj) {
        return !isInt(obj);
    }

    public static boolean isLong(Object obj) {
        if (isNull(obj)) {
            return false;
        }
        try {
            Long.parseLong(obj.toString().trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public static boolean isNotLong(Object obj) {
        return !isLong(obj);
    }

    public static boolean isDouble(Object obj) {
        if (isNull(obj)) {
            return false;
        }
        if (obj instanceof Number) {
            return true;
        }
        try {
            Double.parseDouble(obj.toString().trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public static boolean isNotDouble(Object obj) {
        return !isDouble(obj);
    }

    public static boolean isNumber(Object obj) {
        return isDouble(obj);
    }
    public static boolean isNotNumber(Object obj) {
        return isNotDouble(obj);
    }

    /** 返回当前时间戳(到秒), 相当于 mysql 中的 SELECT UNIX_TIMESTAMP() 语句 */
    public static int unixTimestamp() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    /** 将数值转换成 ipv4, 类似于 mysql 中 INET_NTOA(134744072) ==> 8.8.8.8 */
    public static String num2ip(long num) {
        return ((num & 0xff000000L) >> 24) + "." + ((num & 0xff0000) >> 16)
                + "." + ((num & 0xff00) >> 8) + "." + ((num & 0xff));
    }
    /** 将 ipv4 的地址转换成数值. 类似于 mysql 中 INET_ATON('8.8.8.8') ==> 134744072 */
    public static long ip2num(String ip) {
        long result = 0;
        try {
            for (byte b : java.net.InetAddress.getByName(ip).getAddress()) {
                if ((b & 0x80L) != 0) {
                    result += 256L + b;
                } else {
                    result += b;
                }
                result <<= 8;
            }
            result >>= 8;
        } catch (java.net.UnknownHostException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(String.format("ip(%s)转换成数值时异常", ip), e);
            }
            serviceException("「" + ip + "」不是有效的 ip 地址");
        }
        return result;
    }


    /** 数字格式化千分位: 1234567890.51 ==> 1,234,567,890.51 */
    public static String formatNumberToThousands(String number) {
        if (isBlank(number)) {
            return EMPTY;
        }
        String left, right;
        if (number.contains(".")) {
            int point = number.indexOf(".");
            left = number.substring(0, point);
            right = number.substring(point);
        } else {
            left = number;
            right = EMPTY;
        }
        return THOUSANDS_REGEX.matcher(left).replaceAll("$1,") + right;
    }

    public static String toStr(Object obj) {
        return isNull(obj) ? EMPTY : obj.toString();
    }

    /** 如果字符小于指定的位数, 就在前面补全指定的字符 */
    public static String toStr(Object obj, int minLen, String completion) {
        String str = toStr(obj);
        if (isBlank(str)) {
            return EMPTY;
        }

        int length = str.length();
        if (length >= minLen) {
            return str;
        }
        return toStr(completion).repeat(minLen - length) + str;
    }

    /** 对象长度: 中文字符的长度为 2, 其他字符的长度为 1 */
    public static int toLen(Object obj) {
        if (isNull(obj)) {
            return 0;
        }

        int count = 0;
        String str = obj.toString();
        for (int i = 0; i < str.length(); i++) {
            count += (str.substring(i, i + 1).matches(CHINESE) ? 2 : 1);
        }
        return count;
    }

    /** 字符串转 unicode(字符串转成 \\u 形式的字符串) */
    public static String encodeUnicode(String str) {
        if (isBlank(str)) {
            return str;
        }

        StringBuilder sbd = new StringBuilder();
        for (char c : str.toCharArray()) {
            String hex = Integer.toHexString(c);
            sbd.append("\\u").append(hex.length() <= 2 ? ("00" + hex) : hex);
        }
        return sbd.toString();
    }
    /** unicode 转字符串(\\u 形式的字符串转成字符串) */
    public static String decodeUnicode(String unicode) {
        if (isBlank(unicode)) {
            return unicode;
        }

        StringBuilder sbd = new StringBuilder();
        int i;
        int pos = 0;
        while ((i = unicode.indexOf("\\u", pos)) != -1) {
            sbd.append(unicode, pos, i);
            if (i + 5 < unicode.length()) {
                pos = i + 6;
                sbd.append((char) Integer.parseInt(unicode.substring(i + 2, pos), 16));
            }
        }
        sbd.append(unicode.substring(pos));
        return sbd.toString();
    }

    /** 字符串转 ascii */
    public static String stringToAscii(String str) {
        if (isBlank(str)) {
            return str;
        }

        StringBuilder sbd = new StringBuilder();
        for (char c : str.toCharArray()) {
            sbd.append((int) c).append(" ");
        }
        return sbd.toString();
    }
    /** ascii 转字符串 */
    public static String asciiToString(String ascii) {
        if (isBlank(ascii)) {
            return ascii;
        }

        StringBuilder sbd = new StringBuilder();
        for (String s : ascii.split(" ")) {
            try {
                sbd.append((char) Integer.parseInt(s));
            } catch (NumberFormatException e) {
                sbd.append(s);
            }
        }
        return sbd.toString();
    }

    /** 为 null 则返回默认值 */
    public static <T> T defaultIfNull(T value, T defaultValue) {
        return isNull(value) ? defaultValue : value;
    }

    /** 为 null 或 空白符则返回默认值(字符串) */
    public static String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    /** 不为 null 且 数值大于 0 则返回默认值 */
    public static <T extends Number> T defaultIfGreater0(T value, T defaultValue) {
        return isNotNull(value) && value.doubleValue() > 0 ? value : defaultValue;
    }

    /** 不为 null 且 数值大于等于 0 则返回默认值 */
    public static <T extends Number> T defaultIfGreaterEquals0(T value, T defaultValue) {
        return isNotNull(value) && value.doubleValue() >= 0 ? value : defaultValue;
    }

    /** 为 null 则返回默认值, 否则调用后返回 */
    public static <T, R> R callIfNotNull(T obj, Function<T, R> func) {
        return isNull(obj) ? null : func.apply(obj);
    }
    /** 为 null 则返回默认值, 否则调用后返回 */
    public static <T, R> R callIfNotNull(T obj, Function<T, R> func, R defaultValue) {
        return isNull(obj) ? defaultValue : defaultIfNull(func.apply(obj), defaultValue);
    }

    /** 为 null 或 空白符则返回默认值, 否则调用后返回(字符串) */
    public static <T> String callIfNotBlank(T obj, Function<T, String> func, String defaultValue) {
        return isNull(obj) ? defaultValue : defaultIfBlank(func.apply(obj), defaultValue);
    }

    /** 安全的字符串比较, 时间上是等价的(避免计时攻击) */
    public static boolean safeEquals(String a, String b) {
        if (a != null && b != null) {
            int al = a.length();
            if (al == b.length()) {
                int equal = 0;
                for (int i = 0; i < al; i++) {
                    equal |= a.charAt(i) ^ b.charAt(i);
                }
                return equal == 0;
            }
        }
        return false;
    }

    public static <T> boolean equals(T obj1, T obj2) {
        return Objects.equals(obj1, obj2);
    }
    public static <T> boolean notEquals(T obj1, T obj2) {
        return !equals(obj1, obj2);
    }
    public static boolean equalsIgnoreCase(String str1, String str2) {
        return isNotNull(str1) && str1.equalsIgnoreCase(str2);
    }
    public static boolean notEqualsIgnoreCase(String str1, String str2) {
        return !equalsIgnoreCase(str1, str2);
    }

    /** 空则是 null, 如果是 true、1、on、yes 中的任意一种则是 Boolean 的 True, 否则是 Boolean 的 False */
    public static Boolean getBoolean(Object obj) {
        return isNull(obj) ? null : TRUES.contains(obj.toString().toLowerCase());
    }
    /** 是 true、1、on、yes 中的任意一种则是 boolean 的 true, 否则是 boolean 的 false */
    public static boolean getBool(Object obj) {
        return isNotNull(obj) && TRUES.contains(obj.toString().toLowerCase());
    }

    public static boolean isTrue(Boolean flag) {
        return isNotNull(flag) && flag;
    }
    public static boolean isNotTrue(Boolean flag) {
        return !isTrue(flag);
    }

    /** 对象为 null 时返回 true */
    public static boolean isNull(Object obj) {
        return obj == null;
    }
    /** 对象不为 null 时返回 true */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    /** 为空或是空字符或是 null undefined 时返回 true */
    public static boolean isBlank(String str) {
        if (isNull(str)) {
            return true;
        } else {
            String trim = str.trim();
            return trim.isEmpty() || "null".equalsIgnoreCase(trim) || "undefined".equalsIgnoreCase(trim);
        }
    }
    /** 非空且不是空字符时返回 true */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }


    /**
     * 如果字符长度大于指定长度, 则只输出头尾的固定字符
     * <pre>
     * 「(abcdefghijklmnopqrstuvwxyz1234567890, 20, 3)」 -> 「abc***890[36]」
     * </pre>
     */
    public static String foggyValue(String value, int max, int leftRight) {
        return foggyValue(value, max, leftRight, leftRight, true);
    }
    /**
     * 如果字符长度大于指定长度, 则只输出头尾的固定字符
     * <pre>
     * 「(abcdefghijklmnopqrstuvwxyz1234567890, 20, 0, 0, true)」  -> 「***[36]」
     * 「(abcdefghijklmnopqrstuvwxyz1234567890, 20, 0, 0, false)」 -> 「***」
     * 「(abcdefghijklmnopqrstuvwxyz1234567890, 20, 0, 3, true)」  -> 「***890[36]」
     * 「(abcdefghijklmnopqrstuvwxyz1234567890, 20, 0, 3, false)」 -> 「***890」
     * 「(abcdefghijklmnopqrstuvwxyz1234567890, 20, 3, 0, true)」  -> 「abc***[36]」
     * 「(abcdefghijklmnopqrstuvwxyz1234567890, 20, 3, 0, false)」 -> 「abc***」
     * 「(abcdefghijklmnopqrstuvwxyz1234567890, 20, 3, 3, true)」  -> 「abc***890[36]」
     * 「(abcdefghijklmnopqrstuvwxyz1234567890, 20, 3, 3, false)」 -> 「abc***890」
     * </pre>
     */
    public static String foggyValue(String value, int max, int left, int right, boolean showLen) {
        if (isBlank(value)) {
            return value;
        }

        int valueLen = value.length();
        int lt = (left < 0 || left > valueLen) ? 0 : left;
        int rt = (right < 0 || right > valueLen) ? 0 : right;
        if (valueLen >= max && max > (lt + rt)) {
            if (showLen) {
                return String.format("%s***%s[%s]", value.substring(0, lt), value.substring(valueLen - rt), valueLen);
            } else {
                return String.format("%s***%s", value.substring(0, lt), value.substring(valueLen - rt));
            }
        } else {
            return value;
        }
    }


    /**
     * 验证 指定正则 是否 <span style="color:red;">全字匹配</span> 指定字符串, 匹配则返回 true <br/><br/>
     *
     * 左右空白符 : (?m)(^\s*|\s*$)<br>
     * 空白符 : (^\\s*)|(\\s*$)<br/>
     * 匹配多行注释 : /\*\*(\s|.)*?\* /<br/>
     */
    public static boolean checkRegexWithStrict(String param, String regex) {
        return isNotBlank(param) && Pattern.compile(regex).matcher(param).matches();
    }
    /** 后缀是图片则返回 true */
    public static boolean hasImage(String image) {
        return checkRegexWithStrict(image, IMAGE);
    }
    /** 是正确的邮箱地址则返回 true */
    public static boolean hasEmail(String email) {
        return checkRegexWithStrict(email, EMAIL);
    }
    /** 是一个手机则返回 true */
    public static boolean hasPhone(String phone) {
        return checkRegexWithStrict(phone, PHONE);
    }
    /** 是一个有效的 ip 地址则返回 true */
    public static boolean hasLicitIp(String ip) {
        return checkRegexWithStrict(ip, IPV4);
    }
    /** 是一个有效的身份证号就返回 true */
    public static boolean hasIdCard(String num) {
        return checkRegexWithStrict(num, ID_CARD);
    }
    /** 是本地 ip 则返回 true */
    public static boolean hasLocalRequest(String ip) {
        return checkRegexWithStrict(ip, LOCAL);
    }

    /** 只要找到匹配即返回 true */
    public static boolean checkRegexWithRelax(String param, String regex) {
        return isNotBlank(param) && Pattern.compile(regex).matcher(param).find();
    }
    /** 传入的参数只要包含中文就返回 true */
    public static boolean containsChinese(String value) {
        return checkRegexWithRelax(value, CHINESE);
    }
    /** 传入的参数只要来自移动端就返回 true */
    public static boolean hasMobile(String param) {
        return checkRegexWithRelax(param, MOBILE);
    }
    /** 传入的参数只要是 pc 端就返回 true */
    public static boolean hasPc(String param) {
        return checkRegexWithRelax(param, PC);
    }


    /** 将两个 int 合并成 long */
    public static long merge(int property, int value) {
        return ((long) property << 32) + (long) value;
    }
    /** 将 long 拆分两个 int */
    public static int[] parse(long pv) {
        return new int[] { (int) (pv >> 32), (int) pv };
    }


    /** 字符转义. 主要针对 url 传递给后台前的操作. 如 ? 转换为 %3F, = 转换为 %3D, & 转换为 %26 等 */
    public static String urlEncode(String src) {
        if (isBlank(src)) {
            return EMPTY;
        }
        try {
            // java 中的 encode 是把空格变成 +, 转义后需要将 + 替换成 %2B
            return URLEncoder.encode(src, StandardCharsets.UTF_8.displayName());//.replaceAll("\\+", "%2B");
        } catch (Exception e) {
            return src;
        }
    }
    /** 字符反转义, 主要针对 url 传递到后台后的操作 */
    public static String urlDecode(String src) {
        if (isBlank(src)) {
            return EMPTY;
        }
        try {
            // java 中的 encode 是把空格变成 +, 反转义前需要将 %2B 替换成 +
            return URLDecoder.decode(src/*.replaceAll("%2B", "\\+")*/, StandardCharsets.UTF_8.displayName());
        } catch (Exception e) {
            return src;
        }
    }

    /** 生成不带 - 的 uuid */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", EMPTY);
    }
    /** 生成 16 的 uuid */
    public static String uuid16() {
        return uuid().substring(8, 24);
    }

    /** 获取后缀(包含点 .) */
    public static String getFileSuffix(String file) {
        return (isNotBlank(file) && file.contains(".")) ? file.substring(file.lastIndexOf(".")) : EMPTY;
    }

    /** 将传入的文件重命名成不带 - 的 uuid 名称并返回 */
    public static String renameFile(String fileName) {
        return uuid() + getFileSuffix(fileName);
    }

    /** 为空则返回 /, 如果开头有 / 则直接返回, 否则在开头拼接 / 并返回 */
    public static String addPrefix(String src) {
        if (isBlank(src)) {
            return "/";
        }
        if (src.startsWith("/")) {
            return src;
        }
        return "/" + src;
    }
    /** 为空则返回 /, 如果结尾有 / 则直接返回, 否则在结尾拼接 / 并返回 */
    public static String addSuffix(String src) {
        if (isBlank(src)) {
            return "/";
        }
        if (src.endsWith("/")) {
            return src;
        }
        return src + "/";
    }

    /** 为空则返回空字符串, 如果传入的 url 中有 ? 则在尾部拼接 &, 否则拼接 ? 返回 */
    public static String appendUrl(String src) {
        if (isBlank(src)) {
            return EMPTY;
        }
        return src + (src.contains("?") ? "&" : "?");
    }


    /**
     * 调用对象的属性对应的 get 方法并将返回值用 String 返回(如果是 null 则返回空字符串)
     *
     * @param data  对象
     * @param field 属性名
     * @return 如果属性是枚举则调用枚举的 getValue 方法, 如果是日期则格式化, 否则返回此属性值的 toString 方法
     */
    public static String getFieldMethod(Object data, String field) {
        if (isNull(data) || isBlank(field)) {
            return EMPTY;
        }

        Object value;
        // noinspection rawtypes
        if (data instanceof Map m) {
            value = m.get(field);
        } else {
            value = invokeMethod(data, "get" + field.substring(0, 1).toUpperCase() + field.substring(1));
        }

        if (isNull(value)) {
            return EMPTY;
        } else if (value.getClass().isEnum()) {
            // 如果是枚举, 则调用其 getValue 方法, getValue 没有值则使用枚举的 name
            return toStr(defaultIfNull(invokeMethod(value, "getValue"), value));
        } else if (value instanceof Date d) {
            // 如果是日期, 则格式化
            return toStr(DateUtil.formatDateTime(d));
        } else {
            return toStr(value);
        }
    }

    /** 调用对象的公有方法. 异常将被忽略并返回 null */
    public static Object invokeMethod(Object obj, String method, Object... param) {
        Method m = getMethod(obj, method);
        if (isNotNull(m)) {
            try {
                return m.invoke(obj, param);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("call({}) method({}) exception", obj.getClass().getName(), method, e);
                }
            }
        }
        return null;
    }

    /** 获取对象的所有方法(包括父类) */
    public static List<Method> getMethods(Object obj) {
        Map<String, Method> methodMap = getMethods(obj, 0);
        return methodMap.isEmpty() ? Collections.emptyList() : new ArrayList<>(methodMap.values());
    }
    private static Map<String, Method> getMethods(Object obj, int depth) {
        // noinspection DuplicatedCode
        if (isNull(obj)) {
            return Collections.emptyMap();
        }

        Class<?> clazz = (obj instanceof Class) ? ((Class<?>) obj) : obj.getClass();
        if (clazz == Object.class) {
            return Collections.emptyMap();
        }

        String key = clazz.getName();
        Map<String, Method> methodCacheMap = METHODS_CACHE.get(key);
        if (methodCacheMap != null && !methodCacheMap.isEmpty()) {
            return methodCacheMap;
        }

        Map<String, Method> returnMap = new LinkedHashMap<>();
        for (Method declaredMethod : clazz.getDeclaredMethods()) {
            returnMap.put(declaredMethod.getName(), declaredMethod);
        }
        for (Method method : clazz.getMethods()) {
            returnMap.put(method.getName(), method);
        }

        Class<?> superclass = clazz.getSuperclass();
        if (superclass != Object.class && depth <= MAX_DEPTH) {
            Map<String, Method> methodMap = getMethods(superclass, depth + 1);
            if (!methodMap.isEmpty()) {
                returnMap.putAll(methodMap);
            }
        }
        METHODS_CACHE.put(key, returnMap);
        return returnMap;
    }
    /** 获取对象的指定方法 */
    public static Method getMethod(Object obj, String method) {
        Map<String, Method> methodMap = getMethods(obj, 0);
        return methodMap.isEmpty() ? null : methodMap.get(method);
    }

    /** 获取对象的所有属性(包括父类) */
    public static List<Field> getFields(Object obj) {
        Map<String, Field> fieldMap = getFields(obj, 0);
        return (fieldMap == null || fieldMap.isEmpty()) ? Collections.emptyList() : new ArrayList<>(fieldMap.values());
    }
    private static Map<String, Field> getFields(Object obj, int depth) {
        // noinspection DuplicatedCode
        if (isNull(obj)) {
            return Collections.emptyMap();
        }

        Class<?> clazz = (obj instanceof Class) ? ((Class<?>) obj) : obj.getClass();
        if (clazz == Object.class) {
            return Collections.emptyMap();
        }

        String key = clazz.getName();
        Map<String, Field> fieldCacheMap = FIELDS_CACHE.get(key);
        if (isNotNull(fieldCacheMap)) {
            return fieldCacheMap;
        }

        Map<String, Field> returnMap = new LinkedHashMap<>();
        for (Field declaredField : clazz.getDeclaredFields()) {
            returnMap.put(declaredField.getName(), declaredField);
        }
        for (Field field : clazz.getFields()) {
            returnMap.put(field.getName(), field);
        }

        Class<?> superclass = clazz.getSuperclass();
        if (superclass != Object.class && depth <= MAX_DEPTH) {
            Map<String, Field> fieldMap = getFields(superclass, depth + 1);
            if (fieldMap != null && !fieldMap.isEmpty()) {
                returnMap.putAll(fieldMap);
            }
        }
        FIELDS_CACHE.put(key, returnMap);
        return returnMap;
    }
    /** 获取对象的指定属性 */
    public static Field getField(Object obj, String field) {
        Map<String, Field> fieldMap = getFields(obj, 0);
        return (fieldMap == null || fieldMap.isEmpty()) ? null : fieldMap.get(field);
    }


    /** 将参数 转换成 id=123&name=xyz&name=opq, 将值进行脱敏(如 password=***&phone=130****) */
    public static String formatParam(Map<String, ?> params) {
        return formatParam(true, false, params);
    }
    /** 转换成 id=123&name=xyz&name=opq */
    public static String formatParam(boolean des, boolean encode, Map<String, ?> params) {
        if (A.isEmpty(params)) {
            return EMPTY;
        }

        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            String key = entry.getKey();
            Object obj = entry.getValue();

            String value;
            String split = ",";
            if (obj == null) {
                value = EMPTY;
            } else if (obj.getClass().isArray()) {
                StringJoiner stringJoiner = new StringJoiner(split);
                int len = Array.getLength(obj);
                for (int i = 0; i < len; i++) {
                    Object o = Array.get(obj, i);
                    stringJoiner.add(o == null ? EMPTY : o.toString());
                }
                value = stringJoiner.toString();
            } else if (obj instanceof Collection<?> c) {
                StringJoiner stringJoiner = new StringJoiner(split);
                for (Object o : c) {
                    stringJoiner.add(o == null ? EMPTY : o.toString());
                }
                value = stringJoiner.toString();
            } else {
                value = obj.toString();
            }
            String content = des ? DesensitizationUtil.desByKey(key, value) : value;
            joiner.add(key + "=" + (encode ? U.urlEncode(content) : content));
        }
        return joiner.toString();
    }

    /** 获取指定类所在 jar 包的地址 */
    public static String getClassInFile(Class<?> clazz) {
        if (isNotNull(clazz)) {
            ProtectionDomain domain = clazz.getProtectionDomain();
            if (isNotNull(domain)) {
                CodeSource source = domain.getCodeSource();
                if (isNotNull(source)) {
                    URL location = source.getLocation();
                    if (isNotNull(location)) {
                        return location.toString();
                    }
                }
            }
        }
        return null;
    }

    /**
     * <pre>
     * try (
     *     InputStream input = ...;
     *     OutputStream output = ...;
     * ) {
     *     inputToOutput(input, output);
     * }
     * </pre>
     */
    public static void inputToOutput(InputStream input, OutputStream output) {
        try {
            // guava
            // ByteStreams.copy(inputStream, outputStream);

            // jdk-8
            // byte[] buf = new byte[8192];
            // for (int length; (length = input.read(buf)) != -1; ) {
            //     output.write(buf, 0, length);
            // }

            // jdk-9
            input.transferTo(output);
        } catch (IOException e) {
            throw new RuntimeException("input to output exception", e);
        }
    }

    /**
     * <pre>
     * try (
     *         InputStream input = ...;
     *         OutputStream output = ...;
     * ) {
     *     inputToOutputWithChannel(input, output);
     * }
     * </pre>
     */
    public static void inputToOutputWithChannel(InputStream input, OutputStream output) {
        try (
                ReadableByteChannel read = Channels.newChannel(input);
                WritableByteChannel write = Channels.newChannel(output)
        ) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
            while (read.read(buffer) != -1) {
                buffer.flip();
                write.write(buffer);
                buffer.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException("read to output with channel exception", e);
        }
    }

    /** 将长字符串进行 gzip 压缩, 再转成 base64 编码返回 */
    public static String compress(final String str) {
        if (isNull(str)) {
            return null;
        }

        String trim = str.trim();
        if (EMPTY.equals(trim) || trim.length() < COMPRESS_MIN_LEN) {
            return trim;
        }

        try (
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                GZIPOutputStream gzip = new GZIPOutputStream(output)
        ) {
            gzip.write(trim.getBytes(StandardCharsets.UTF_8));
            gzip.finish();
            return new String(Base64.getEncoder().encode(output.toByteArray()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("压缩字符串异常", e);
            }
            return trim;
        }
    }
    /** 将压缩的字符串用 base64 解码, 再进行 gzip 解压 */
    public static String decompress(String str) {
        if (isNull(str)) {
            return null;
        }
        String trim = str.trim();
        if (EMPTY.equals(trim)) {
            return trim;
        }
        // 如果字符串以 { 开头且以 } 结尾, 或者以 [ 开头以 ] 结尾(json)则不解压, 直接返回
        if ((str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]"))) {
            return trim;
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8));
            if (bytes.length == 0) {
                return trim;
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("字符串解码异常", e);
            }
            return trim;
        }
        try (
                ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                GZIPInputStream gis = new GZIPInputStream(input);

                InputStreamReader in = new InputStreamReader(gis, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(in)
        ) {
            StringBuilder sbd = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sbd.append(line);
            }
            return sbd.toString();
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("解压字符串异常", e);
            }
            return trim;
        }
    }


    /** 对象为 null 时抛出异常 */
    public static void assertNil(Object obj, String msg) {
        if (isNull(obj)) {
            serviceException(msg);
        }
    }

    /** 空白符、null、undefined、nil 时抛出异常 */
    public static void assertBlank(String str, String msg) {
        if (isBlank(str)) {
            serviceException(msg);
        }
    }

    /** 数组为 null 或 长度为 0 时则抛出异常 */
    public static <T> void assertEmpty(T[] array, String msg) {
        if (array == null || array.length == 0) {
            serviceException(msg);
        }
    }
    /** 列表为 null 或 长度为 0 时则抛出异常 */
    public static <T> void assertEmpty(Collection<T> list, String msg) {
        if (list == null || list.isEmpty()) {
            serviceException(msg);
        }
    }
    /** map 为 null 或 长度为 0 时则抛出异常 */
    public static <K, V> void assertEmpty(Map<K, V> map, String msg) {
        if (map == null || map.isEmpty()) {
            serviceException(msg);
        }
    }

    /** 数值为空或小于等于 0 则抛出异常 */
    public static void assert0(Number number, String msg) {
        if (lessAndEquals0(number)) {
            serviceException(msg);
        }
    }

    /** 数值为空或小于 0 则抛出异常 */
    public static void assertLess0(Number number, String msg) {
        if (less0(number)) {
            serviceException(msg);
        }
    }

    /** 条件为 true 则抛出业务异常 */
    public static void assertException(boolean flag, String msg) {
        if (flag) {
            serviceException(msg);
        }
    }

    /** 国际化 */
    public static void assertI18nException(String code, Object... args) {
        throw new ServiceI18nException(code, args);
    }

    /** 错误的请求 */
    public static void badRequestException(String msg) {
        throw new BadRequestException(msg);
    }
    /** 需要权限 */
    public static void forbiddenException(String msg) {
        throw new ForbiddenException(msg);
    }
    /** 404 */
    public static void notFoundException(String msg) {
        throw new NotFoundException(msg);
    }
    /** 未登录 */
    public static void notLoginException() {
        throw new NotLoginException();
    }
    /** 业务异常 */
    public static void serviceException(String msg) {
        throw new ServiceException(msg);
    }
}
