package com.github.common.util;

import com.github.common.Money;
import com.github.common.date.DateUtil;
import com.github.common.exception.*;
import com.github.common.json.JsonUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** 工具类 */
public final class U {

    private static final Logger LOGGER = LoggerFactory.getLogger(U.class);
    private static final Pattern MULTI_SPACE_REGEX = Pattern.compile("\\s{2,}");

    public static final Random RANDOM = new Random();

    /** 本机的 cpu 核心数 */
    public static final int PROCESSORS = Runtime.getRuntime().availableProcessors();

    public static final String EMPTY = "";
    public static final String BLANK = " ";
    private static final String LIKE = "%";

    /** 手机号. 见: https://zh.wikipedia.org/wiki/%E4%B8%AD%E5%9B%BD%E5%86%85%E5%9C%B0%E7%A7%BB%E5%8A%A8%E7%BB%88%E7%AB%AF%E9%80%9A%E8%AE%AF%E5%8F%B7%E6%AE%B5 */
    private static final String PHONE = "^1[3-9]\\d{9}$";
    /** _abc-def@123-hij.uvw_xyz.com 是正确的, -123@xyz.com 不是 */
    private static final String EMAIL = "^\\w[\\w\\-]*@([\\w\\-]+\\.\\w+)+$";
    /** ico, jpeg, jpg, bmp, png 后缀 */
    private static final String IMAGE = "(?i)^(.*)\\.(ico|jpeg|jpg|bmp|png)$";
    /** IPv4 地址 */
    private static final String IPV4 = "^([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])(\\.([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])){3}$";
    /** 身份证号码 */
    private static final String ID_CARD = "(^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$)|(^[1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}$)";

    /** 字母 */
    private static final String LETTER = "(?i)[a-z]";
    /** 中文 */
    private static final String CHINESE = "[\\u4e00-\\u9fa5]";
    /** 是否是移动端: https://gist.github.com/dalethedeveloper/1503252 */
    private static final String MOBILE = "(?i)Mobile|iP(hone|od|ad)|Android|BlackBerry|Blazer|PSP|UCWEB|IEMobile|Kindle|NetFront|Silk-Accelerated|(hpw|web)OS|Fennec|Minimo|Opera M(obi|ini)|Dol(f|ph)in|Skyfire|Zune";
    /** 是否是 iOS 端 */
    private static final String IOS = "(?i)iP(hone|od|ad)";
    /** 是否是 android 端 */
    private static final String ANDROID = "(?i)Mobile|Android";
    /** 是否是 pc 端 */
    private static final String PC = "(?i)AppleWebKit|Mozilla|Chrome|Safari|MSIE|Windows NT";
    /** 是否是本地 ip */
    private static final String LOCAL = "(?i)127.0.0.1|localhost|::1|0:0:0:0:0:0:0:1";

    /**
     * 字符串长度 >= 这个值, 才进行压缩
     *
     * 字符串压缩时, 将字符串先操作 gzip 再编码成 base64 字符串
     * 解压字符串时, 将字符串先解码 base64 再操作 gzip 解压
     */
    private static final int COMPRESS_MIN_LEN = 1000;

    private static final String TMP = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

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

    // ========== enum ==========
    /**
     * 获取枚举中的值, 先匹配 name, 再匹配 getCode(数字), 再匹配 getValue(中文), 都匹配不上则返回 null
     *
     * @param clazz 枚举的类信息
     * @param obj 要匹配的值
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Enum> E toEnum(Class<E> clazz, Object obj) {
        if (isNotBlank(obj)) {
            E[] constants = clazz.getEnumConstants();
            if (constants != null && constants.length > 0) {
                String source = obj.toString().trim();
                for (E em : constants) {
                    // 如果传递过来的是枚举名, 且能匹配上则返回
                    if (source.equalsIgnoreCase(em.name())) {
                        return em;
                    }

                    // 如果传递过来的值跟枚举的 getCode(数字) 相同则返回
                    Object code = getMethod(em, "getCode");
                    if (isNotBlank(code) && source.equalsIgnoreCase(code.toString().trim())) {
                        return em;
                    }

                    // 如果传递过来的值跟枚举的 getValue(中文) 相同则返回
                    Object value = getMethod(em, "getValue");
                    if (isNotBlank(value) && source.equalsIgnoreCase(value.toString().trim())) {
                        return em;
                    }

                    // 如果传递过来的值跟枚举的 ordinal(数字. 表示枚举所在的索引) 相同则返回
                    // if (source.equalsIgnoreCase(String.valueOf(em.ordinal()))) return em;
                }
            }
        }
        return null;
    }

    private static final String ENUM_CODE = "code";
    private static final String ENUM_VALUE = "value";
    /**
     * <pre>
     * 序列化枚举, 如以下示例
     *
     * public enum Gender {
     *   Male(0, "男"), Female(1, "女");
     *
     *   &#064;EnumValue
     *   private final int code;
     *   private final String value;
     *
     *   &#064;JsonValue
     *   public Map<String, String> serializer() {
     *     return <span style="color:red">serializerEnum(code, value);</span>
     *   }
     *   &#064;JsonCreator
     *   public static Gender deserializer(Object obj) {
     *     return enumDeserializer(obj, Gender.class);
     *   }
     * }
     *
     * Gender.Male 在序列化时将返回 { "code": 0, "value": "男" } 其中 code 用来交互, value 用来显示
     * 反序列化时, 0、男、{ "code": 0, "value": "男" } 都可以反序列化成 Gender.Male
     * </pre>
     */
    public static Map<String, String> serializerEnum(int code, String value) {
        return A.maps(ENUM_CODE, code, ENUM_VALUE, value);
    }
    /**
     * <pre>
     * 枚举反序列化, 如以下示例
     *
     * public enum Gender {
     *   Male(0, "男"), Female(1, "女");
     *
     *   &#064;EnumValue
     *   private final int code;
     *   private final String value;
     *
     *   &#064;JsonValue
     *   public Map<String, String> serializer() {
     *     return <span style="color:red">serializerEnum(code, value);</span>
     *   }
     *   &#064;JsonCreator
     *   public static Gender deserializer(Object obj) {
     *     return enumDeserializer(obj, Gender.class);
     *   }
     * }
     *
     * Gender.Male 在序列化时将返回 { "code": 0, "value": "男" } code 用来交互, value 用来显示
     * 而 0、男、{ "code": 0, "value": "男" } 也都可以反序列化成 Gender.Male
     * </pre>
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Enum> E enumDeserializer(Object obj, Class<E> enumClass) {
        if (isNull(obj)) {
            return null;
        }

        Object tmp = null;
        if (obj instanceof Map) {
            tmp = getEnumInMap((Map) obj);
        } else {
            String tmpStr = obj.toString().trim();
            if (tmpStr.startsWith("{") && tmpStr.endsWith("}")) {
                tmp = getEnumInMap(JsonUtil.toObjectNil(obj.toString(), Map.class));
            }
        }

        if (isNull(tmp)) {
            tmp = obj;
        }
        return toEnum(enumClass, tmp);
    }
    @SuppressWarnings("rawtypes")
    private static Object getEnumInMap(Map map) {
        if (A.isNotEmpty(map)) {
            Object tmp = map.get(ENUM_CODE);
            if (isNull(tmp)) {
                tmp = map.get(ENUM_VALUE);
            }
            return tmp;
        } else {
            return null;
        }
    }
    // ========== enum ==========


    // ========== number ==========
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
        if (isNull(obj)) {
            return 0;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 转换成 long, 非数字则返回 0L */
    public static long toLong(Object obj) {
        if (isNull(obj)) {
            return 0L;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** 转换成 float, 非数字则返回 0F */
    public static float toFloat(Object obj) {
        if (isNull(obj)) {
            return 0F;
        }
        if (obj instanceof Number) {
            return ((Number) obj).floatValue();
        }
        try {
            return Float.parseFloat(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0F;
        }
    }

    /** 转换成 double, 非数字则返回 0D */
    public static double toDouble(Object obj) {
        if (isNull(obj)) {
            return 0D;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0D;
        }
    }

    /** 是数字则返回 true */
    public static boolean isNumber(Object obj) {
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

    /** 不是数字则返回 true */
    public static boolean isNotNumber(Object obj) {
        return !isNumber(obj);
    }

    /** 将数值转换成 ipv4, 类似于 mysql 中 INET_NTOA(134744072) ==> 8.8.8.8 */
    public static String num2ip(long num) {
        return ((num & 0xff000000) >> 24) + "." + ((num & 0xff0000) >> 16)
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
            assertException("「" + ip + "」不是有效的 ip 地址");
        }
        return result;
    }
    // ========== number ==========


    // ========== object & string ==========
    private static final Pattern THOUSANDS_REGEX = Pattern.compile("(\\d)(?=(?:\\d{3})+$)");
    /** 1234567890.51 ==> 1,234,567,890.51 */
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

    public static boolean equals(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        } else if (obj1 != null && obj2 != null) {
            String s1 = obj1.toString();
            String s2 = obj2.toString();
            if (s1.length() != s2.length()) {
                return false;
            } else {
                return s1.equals(s2);
            }
        } else {
            return false;
        }
    }
    public static boolean equalsIgnoreCase(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        } else if (obj1 != null && obj2 != null) {
            String s1 = obj1.toString();
            String s2 = obj2.toString();
            if (s1.length() != s2.length()) {
                return false;
            } else {
                return s1.equalsIgnoreCase(s2);
            }
        } else {
            return false;
        }
    }

    public static String toStr(Object obj) {
        return isNull(obj) ? EMPTY : obj.toString();
    }

    /** 如果字符长度大于指定长度, 则只输出头尾的固定字符 */
    public static String toStr(Object obj, int maxLen, int leftRightLen) {
        String str = toStr(obj);
        if (isBlank(str)) {
            return EMPTY;
        }

        int length = str.length();
        if (length > maxLen) {
            int returnLength = leftRightLen * 2 + 5;
            if (maxLen > returnLength) {
                return str.substring(0, leftRightLen) + " ... " + str.substring(length - leftRightLen, length);
            }
        }
        return str;
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

        StringBuilder sbd = new StringBuilder();
        int loop = minLen - length;
        for (int i = 0; i < loop; i++) {
            sbd.append(completion);
        }
        return sbd.append(str).toString();
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

    /** 字符串转 unicode(中文转成 \\u) */
    public static String encodeUnicode(String str) {
        if (isEmpty(str)) {
            return str;
        }

        StringBuilder sbd = new StringBuilder();
        for (char c : str.toCharArray()) {
            String hex = Integer.toHexString(c);
            sbd.append("\\u").append(hex.length() <= 2 ? ("00" + hex) : hex);
        }
        return sbd.toString();
    }

    /** unicode 转字符串(\\u 转成中文) */
    public static String decodeUnicode(String unicode) {
        if (isEmpty(unicode)) {
            return unicode;
        }

        StringBuilder sb = new StringBuilder();
        int i;
        int pos = 0;
        while ((i = unicode.indexOf("\\u", pos)) != -1) {
            sb.append(unicode, pos, i);
            if (i + 5 < unicode.length()) {
                pos = i + 6;
                sb.append((char) Integer.parseInt(unicode.substring(i + 2, i + 6), 16));
            }
        }
        sb.append(unicode.substring(pos));
        return sb.toString();
    }

    /** 字符串转 ascii */
    public static String stringToAscii(String str) {
        if (isEmpty(str)) {
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
        if (isEmpty(ascii)) {
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

    /** 去掉所有的制表符 和 换行符 */
    public static String replaceTabAndWrap(String str) {
        return isBlank(str) ? EMPTY : str.replace("\t", EMPTY).replace("\n", EMPTY);
    }
    /** 将字符串中的多个空白符替换成一个 */
    public static String replaceBlank(String str) {
        return isBlank(str)
                ? EMPTY
                : MULTI_SPACE_REGEX.matcher(str.replace("\r", EMPTY).replace("\n", BLANK)).replaceAll(BLANK).trim();
    }

    public static <T> T defaultIfNull(T obj, T defaultValue) {
        return isNull(obj) ? defaultValue : obj;
    }

    /** 对象为 null 时返回 true */
    public static boolean isNull(Object obj) {
        return obj == null;
    }
    /** 对象不为 null 时返回 true */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    /** 对象为空 或 其字符串形态是空字符 时返回 true */
    public static boolean isBlank(Object obj) {
        if (isNull(obj)) {
            return true;
        }

        String str = obj.toString().trim();
        return EMPTY.equals(str);
    }
    /** 对象非空 且 其字符串形态不是空字符 时返回 true */
    public static boolean isNotBlank(Object obj) {
        return !isBlank(obj);
    }

    /** 对象为空, 字符串为 null nil undefined, 数组、集合、Map 长度为 0 则返回 true */
    public static boolean isEmpty(Object obj) {
        if (isNull(obj)) {
            return true;
        }

        if (obj instanceof CharSequence) {
            String str = obj.toString().trim();
            return str.isEmpty()
                    || "null".equalsIgnoreCase(str)
                    || "nil".equalsIgnoreCase(str)
                    || "undefined".equalsIgnoreCase(str);
        } else if (obj instanceof Optional) {
            return !((Optional<?>) obj).isPresent();
        } else if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        } else if (obj instanceof Collection) {
            return ((Collection<?>) obj).isEmpty();
        } else if (obj instanceof Map) {
            return ((Map<?, ?>) obj).isEmpty();
        } else {
            return false;
        }
    }
    /** 对象非空 且 其字符串形态不是 空白符、null、undefined 时返回 true */
    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    /** 对象长度在指定的数值以内(不包含边距)就返回 true */
    public static boolean length(Object obj, int max) {
        return obj != null && obj.toString().trim().length() < max;
    }

    /** 对象长度在指定的数值经内(包含边距)就返回 true */
    public static boolean lengthBorder(String str, int min, int max) {
        return !isBlank(str) && str.length() >= min && str.length() <= max;
    }
    /** 对象长度在指定的数值以内(不包含边距)就返回 true */
    public static boolean length(String str, int min, int max) {
        return lengthBorder(str, min + 1, max - 1);
    }

    private static final Pattern ALL = Pattern.compile(".");
    /** 将字符串中指定位数的值模糊成 * 并返回. 索引位从 0 开始 */
    public static String foggy(String param, int start, int end) {
        if (isBlank(param)) {
            return EMPTY;
        }
        if (start < 0 || end < start || end > param.length()) {
            return param;
        }
        return param.substring(0, start) + ALL.matcher(param.substring(start, end)).replaceAll("*") + param.substring(end);
    }
    public static String foggyPhone(String phone) {
        return checkPhone(phone) ? phone.substring(0, 3) + " **** " + phone.substring(phone.length() - 4) : phone;
    }
    public static String foggyIdCard(String idCard) {
        // 是标准的 15 或 18 位身份证就返回「前面 3 位 + 后面 2 位」. 只有 6 位尾数则只返回「后面 2 位」
        if (isBlank(idCard)) {
            return idCard;
        } else if (isIdCard(idCard)) {
            return idCard.substring(0, 3) + " **** " + idCard.substring(idCard.length() - 2);
        } else if (idCard.length() == 6) {
            return "**** " + idCard.substring(2);
        } else {
            return idCard;
        }
    }

    public static String like(String param) {
        return isBlank(param) ? EMPTY : LIKE + param + LIKE;
    }
    public static String leftLike(String param) {
        return isBlank(param) ? EMPTY : LIKE + param;
    }
    public static String rightLike(String param) {
        return isBlank(param) ? EMPTY : param + LIKE;
    }
    // ========== object & string ==========


    // ========== regex ==========
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
    public static boolean checkImage(String image) {
        return checkRegexWithStrict(image, IMAGE);
    }
    /** 是正确的邮箱地址则返回 true */
    public static boolean checkEmail(String email) {
        return checkRegexWithStrict(email, EMAIL);
    }
    /** 是一个手机则返回 true. 可以使用 - 和空格. 131-1234-5678, 131 1234 5678 是正确的手机号, 131-1234 5678 不是 */
    public static boolean checkPhone(String phone) {
        return checkRegexWithStrict(phone, PHONE);
    }
    /** 是一个有效的 ip 地址则返回 true */
    public static boolean isLicitIp(String ip) {
        return checkRegexWithStrict(ip, IPV4);
    }
    /** 是一个有效的身份证号就返回 true */
    public static boolean isIdCard(String num) {
        return checkRegexWithStrict(num, ID_CARD);
    }
    /** 基于身份证号码返回性别 */
    public static String idCardToGender(String num) {
        if (isIdCard(num)) {
            int len = num.length();
            String g;
            if (len == 15) {
                g = num.substring(14, 15);
            } else if (len == 18) {
                g = num.substring(16, 17);
            } else {
                g = EMPTY;
            }
            return isNumber(g) ? (toInt(g) % 2 == 0 ? "女" : "男") : "未知";
        } else {
            return "未知";
        }
    }
    /** 是本地请求则返回 true */
    public static boolean isLocalRequest(String ip) {
        return checkRegexWithStrict(ip, LOCAL);
    }

    /** 只要找到匹配即返回 true */
    public static boolean checkRegexWithRelax(String param, String regex) {
        return isNotBlank(param) && Pattern.compile(regex).matcher(param).find();
    }

    /** 传入的参数只要包含字母就返回 true */
    public static boolean isLetter(String param) {
        return checkRegexWithRelax(param, LETTER);
    }
    /** 传入的参数只要包含中文就返回 true */
    public static boolean checkChinese(String param) {
        return checkRegexWithRelax(param, CHINESE);
    }
    /** 传入的参数只要来自移动端就返回 true */
    public static boolean checkMobile(String param) {
        return checkRegexWithRelax(param, MOBILE);
    }
    /** 传入的参数只要是 iOS 端就返回 true */
    public static boolean checkiOS(String param) {
        return checkRegexWithRelax(param, IOS);
    }
    /** 传入的参数只要是 android 端就返回 true */
    public static boolean checkAndroid(String param) {
        return checkRegexWithRelax(param, ANDROID);
    }
    /** 传入的参数只要是 pc 端就返回 true */
    public static boolean checkPc(String param) {
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
    /** 给参数中的值转义 */
    public static String urlEncodeValue(String src) {
        if (isBlank(src)) {
            return EMPTY;
        }

        if (!src.contains("=")) {
            return src;
        }
        StringBuilder sbd = new StringBuilder();
        String[] sp = src.split("&");
        for (int i = 0; i < sp.length; i++) {
            String[] split = sp[i].split("=");

            if (i > 0) {
                sbd.append("&");
            }
            sbd.append(split[0]).append("=");
            if (split.length == 2) {
                sbd.append(urlEncode(split[1]));
            }
        }
        return sbd.toString();
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
    public static String getSuffix(String file) {
        return (isNotBlank(file) && file.contains("."))
                ? file.substring(file.lastIndexOf(".")) : EMPTY;
    }

    /** 将传入的文件重命名成不带 - 的 uuid 名称并返回 */
    public static String renameFile(String fileName) {
        return uuid() + getSuffix(fileName);
    }

    /** 拼接 url 和 path */
    public static String appendUrlAndPath(String url, String path) {
        return addSuffix(url) + addPrefix(path).substring(1);
    }
    /** 为空则返回空字符串, 如果传入的 url 中有 ? 则在尾部拼接 &, 否则拼接 ? 返回 */
    public static String appendUrl(String src) {
        if (isBlank(src)) {
            return EMPTY;
        }
        return src + (src.contains("?") ? "&" : "?");
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
    /** 从 url 中获取最后一个 斜杆(/) 后的内容 */
    public static String getFileNameInUrl(String url) {
        if (isBlank(url) || !url.contains("/")) {
            return EMPTY;
        }
        // 只截取到 ? 处, 如果有的话
        int last = url.contains("?") ? url.lastIndexOf("?") : url.length();
        return url.substring(url.lastIndexOf("/") + 1, last);
    }

    /** 属性转换成方法, 加上 get 并首字母大写 */
    public static String fieldToMethod(String field) {
        if (isBlank(field)) {
            return EMPTY;
        }
        field = field.trim();
        if (isBlank(field)) {
            return EMPTY;
        }
        return  "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    /**
     * 调用对象的属性对应的 get 方法
     *
     * @param data  对象
     * @param field 属性名
     * @return 如果属性是枚举则调用枚举的 getValue 方法, 如果是日期则格式化, 否则返回此属性值的 toString 方法
     */
    @SuppressWarnings("rawtypes")
    public static String getField(Object data, String field) {
        if (isNull(data) || isBlank(field)) {
            return EMPTY;
        }

        String[] split = field.split("\\|");
        Object value;
        if (data instanceof Map) {
            value = ((Map) data).get(split[0]);
        } else {
            try {
                value = new PropertyDescriptor(field, data.getClass()).getReadMethod().invoke(data);
            } catch (Exception e) {
                value = getMethod(data, fieldToMethod(split[0]));
            }
        }

        if (isNull(value)) {
            Class<?> fieldType = getFieldType(data, field);
            return (isNotBlank(fieldType) && fieldType == Money.class) ? "0" : EMPTY;
        } else if (value.getClass().isEnum()) {
            // 如果是枚举, 则调用其 getValue 方法, getValue 没有值则使用枚举的 name
            Object enumValue = getMethod(value, "getValue");
            return toStr(enumValue != null ? enumValue : value);
        } else if (value instanceof Date) {
            // 如果是日期, 则格式化
            if (split.length > 1 && isNotBlank(split[1])) {
                return toStr(DateUtil.format((Date) value, split[1]));
            } else {
                return toStr(DateUtil.formatDateTime((Date) value));
            }
        } else {
            return toStr(value);
        }
    }

    /** 获取类的所有属性(包括父类) */
    public static Set<Field> getAllField(Class<?> clazz) {
        if (clazz.getName().equals(Object.class.getName())) {
            return Collections.emptySet();
        }

        Set<Field> fieldSet = Sets.newLinkedHashSet();
        fieldSet.addAll(Arrays.asList(clazz.getDeclaredFields()));
        fieldSet.addAll(Arrays.asList(clazz.getFields()));

        Class<?> superclass = clazz.getSuperclass();
        if (superclass.getName().equals(Object.class.getName())) {
            return fieldSet;
        }

        Set<Field> tmpSet = getAllField(superclass);
        if (tmpSet.size() > 0) {
            fieldSet.addAll(tmpSet);
        }
        return fieldSet;
    }

    /** 获取类的所有方法(包括父类) */
    public static Set<Method> getAllMethod(Class<?> clazz) {
        if (clazz.getName().equals(Object.class.getName())) {
            return Collections.emptySet();
        }

        Set<Method> methodSet = Sets.newLinkedHashSet();
        methodSet.addAll(Arrays.asList(clazz.getDeclaredMethods()));
        methodSet.addAll(Arrays.asList(clazz.getMethods()));

        Class<?> superclass = clazz.getSuperclass();
        if (superclass.getName().equals(Object.class.getName())) {
            return methodSet;
        }

        Set<Method> tmpSet = getAllMethod(superclass);
        if (tmpSet.size() > 0) {
            methodSet.addAll(tmpSet);
        }
        return methodSet;
    }

    /**
     * <pre>
     * 将 source 中字段的值填充到 target 中, 如果已经有值了就忽略 set
     *
     * if (target.getXyz() != null) {
     *     Object xyz = source.getXyz();
     *     if (xyz != null) {
     *         target.setXyz(xyz);
     *     }
     * }
     *
     * PS: 字段类型如果是基础数据类型, 会有默认值: boolean 是 false, int、long、float、double 是 0
     * 因此这时候判断不是空(上面的 target.getXyz() != null)时将总是返回 true, 因此请使用包装类型
     * </pre>
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static <S,T> void fillData(S source, T target) {
        fillData(source, target, true);
    }

    /**
     * 将 source 中字段的值填充到 target 中
     *
     * if (ignoreAlready) {
     *     if (target.getXyz() != null) {
     *         Object xyz = source.getXyz();
     *         if (xyz != null) {
     *             target.setXyz(xyz);
     *         }
     *     }
     * } else {
     *     Object xyz = source.getXyz();
     *     if (xyz != null) {
     *         target.setXyz(xyz);
     *     }
     * }
     *
     * @param source 源对象
     * @param target 目标对象
     * @param ignoreAlready true: target 中的字段有值则不进行 set 操作
     */
    public static <S,T> void fillData(S source, T target, boolean ignoreAlready) {
        Class<?> tc = target.getClass();
        Method[] targetMethods = tc.getMethods();

        Map<String, Method> targetMethodMap = Maps.newHashMap();
        for (Method method : targetMethods) {
            targetMethodMap.put(method.getName(), method);
        }

        Class<?> sc = source.getClass();
        Method[] sourceMethods = sc.getMethods();
        Map<String, Method> sourceMethodMap = Maps.newHashMap();
        for (Method method : sourceMethods) {
            sourceMethodMap.put(method.getName(), method);
        }
        for (Method method : targetMethods) {
            String methodName = method.getName();
            if (methodName.startsWith("set")) {
                // boolean 的 get 方法是 isXxx(), 其他(包括 Boolean)都是 getXxx()
                String getMethodName = method.getReturnType().getName().equals(boolean.class.getName())
                        ? ("is" + methodName.substring(2)) : ("get" + methodName.substring(3));
                if (sourceMethodMap.containsKey(getMethodName)) {
                    if (ignoreAlready) {
                        try {
                            Object oldResult = targetMethodMap.get(getMethodName).invoke(target);
                            if (oldResult != null) {
                                continue;
                            }
                        } catch (Exception e) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(String.format("%s invoke %s exception", tc.getName(), getMethodName), e);
                            }
                            continue;
                        }
                    }

                    Object targetObj;
                    try {
                        targetObj = sourceMethodMap.get(getMethodName).invoke(source);
                    } catch (Exception e) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(String.format("%s invoke %s exception", sc.getName(), getMethodName), e);
                        }
                        continue;
                    }
                    if (targetObj != null) {
                        try {
                            method.invoke(target, targetObj);
                        } catch (Exception e) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(String.format("%s invoke %s exception", tc.getName(), getMethodName), e);
                            }
                        }
                    }
                }
            }
        }
    }

    /** 调用对象的公有方法. 异常将被忽略并返回 null */
    public static Object getMethod(Object obj, String method, Object... param) {
        if (isNotBlank(method)) {
            try {
                return obj.getClass().getDeclaredMethod(method).invoke(obj, param);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignore) {
            }
            // getMethod 会将从父类继承过来的 public 方法也查询出来
            try {
                return obj.getClass().getMethod(method).invoke(obj, param);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignore) {
            }
        }
        return null;
    }

    public static Class<?> getFieldType(Object obj, String field) {
        if (isEmpty(field)) {
            return null;
        }
        Class<?> clazz = obj.getClass();
        try {
            Field f = clazz.getDeclaredField(field);
            if (isNotBlank(f)) {
                return f.getType();
            }
        } catch (NoSuchFieldException ignore) {
        }
        try {
            Field f = clazz.getField(field);
            if (isNotBlank(f)) {
                return f.getType();
            }
        } catch (NoSuchFieldException ignore) {
        }

        Class<?> superclass = clazz.getSuperclass();
        if (superclass == Object.class) {
            return null;
        }
        return getFieldType(superclass, field);
    }

    /** 转换成 id=123&name=xyz&name=opq */
    public static String formatParam(Map<String, ?> params) {
        if (A.isEmpty(params)) {
            return EMPTY;
        }

        StringBuilder sbd = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (isNotBlank(key) && isNotBlank(value)) {
                if (i > 0) {
                    sbd.append("&");
                }
                String v = "password".equalsIgnoreCase(key) ? "***" : A.toString(value);
                sbd.append(key).append("=").append(v);
                i++;
            }
        }
        return sbd.toString();
    }

    /** 获取指定类所在 jar 包的地址 */
    public static String getClassInFile(Class<?> clazz) {
        if (isNotBlank(clazz)) {
            ProtectionDomain domain = clazz.getProtectionDomain();
            if (isNotBlank(domain)) {
                CodeSource source = domain.getCodeSource();
                if (isNotBlank(source)) {
                    URL location = source.getLocation();
                    if (isNotBlank(location)) {
                        return location.getFile();
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
            byte[] buf = new byte[8192];
            int length;
            while ((length = input.read(buf)) != -1) {
                output.write(buf, 0, length);
            }
            // jdk-9
            // inputStream.transferTo(outputStream);
        } catch (IOException e) {
            throw new RuntimeException("input to output exception", e);
        }
    }

    /**
     * <pre>
     * try (
     *         InputStream inputStream = ...;
     *         OutputStream outputStream = ...;
     *
     *         ReadableByteChannel input = Channels.newChannel(inputStream);
     *         WritableByteChannel output = Channels.newChannel(outputStream);
     * ) {
     *     inputToOutputWithChannel(input, output);
     * }
     * </pre>
     */
    public static void inputToOutputWithChannel(ReadableByteChannel input, WritableByteChannel output) {
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
            while (input.read(buffer) != -1) {
                buffer.flip();
                output.write(buffer);
                buffer.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException("input to output with channel exception", e);
        }
    }

    /** 将长字符串进行 gzip 压缩后再转成 base64 编码返回 */
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
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("压缩字符串异常", e);
            }
            return trim;
        }
    }

    /** 将压缩的字符串用 base64 解码再进行 gzip 解压 */
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
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("字符串解码异常", e);
            }
            return trim;
        }
        if (bytes.length == 0) {
            return trim;
        }

        try (
                ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                GZIPInputStream gis = new GZIPInputStream(input);

                InputStreamReader in = new InputStreamReader(gis, StandardCharsets.UTF_8);
                final BufferedReader bufferedReader = new BufferedReader(in)
        ) {
            StringBuilder sbd = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sbd.append(line);
            }
            return sbd.toString();
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("解压字符串异常", e);
            }
            return trim;
        }
    }


    /** 对象为 null、空白符、"null" 字符串时, 则抛出异常 */
    public static void assertNil(Object obj, String msg) {
        if (isNull(obj)) {
            assertException(msg);
        }
    }

    /** 数组为 null 或 长度为 0 时则抛出异常 */
    public static <T> void assertEmpty(T[] array, String msg) {
        if (A.isEmpty(array)) {
            assertException(msg);
        }
    }

    /** 列表为 null 或 长度为 0 时则抛出异常 */
    public static <T> void assertEmpty(Collection<T> list, String msg) {
        if (A.isEmpty(list)) {
            assertException(msg);
        }
    }

    /** map 为 null 或 长度为 0 时则抛出异常 */
    public static <K,V> void assertEmpty(Map<K,V> map, String msg) {
        if (A.isEmpty(map)) {
            assertException(msg);
        }
    }

    /** 数值为空或小于等于 0 则抛出异常 */
    public static void assert0(Number number, String msg) {
        if (lessAndEquals0(number)) {
            assertException(msg);
        }
    }

    /** 条件为 true 则抛出业务异常 */
    public static void assertException(Boolean flag, String msg) {
        if (flag != null && flag) {
            assertException(msg);
        }
    }

    public static void assertException(String msg) {
        serviceException(msg);
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

    /** 条件为 true 则抛出必须处理的异常 */
    public static void assertMustHandleException(Boolean flag, String msg) throws ServiceMustHandleException {
        if (flag != null && flag) {
            throw new ServiceMustHandleException(msg);
        }
    }

    public static String returnMsg(Throwable e, boolean online) {
        String msg;
        if (online) {
            msg = "出" + (e instanceof NullPointerException ? "问题" : "错") + "了, 但别担心, 这不是你的错.";
        } else if (e instanceof NullPointerException) {
            msg = "空指针异常, 联系后台查看日志进行处理";
        } else {
            msg = e.getMessage();
        }
        return msg;
    }
}
