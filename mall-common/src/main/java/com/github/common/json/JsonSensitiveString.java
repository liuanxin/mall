package com.github.common.json;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.common.util.U;

import java.lang.annotation.*;

/** 脱敏注解. 只能标在 String 类型上 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JsonSerialize(using = JsonSensitiveSerializer.class)
@JacksonAnnotationsInside
public @interface JsonSensitiveString {

    Strategy value();

    /** true 则在脱敏的 *** 后加上 [长度], 如 abcde -> a***[4]    13012345678 -> 130***[4]5678 */
    boolean showLen() default false;


    enum Strategy {
        /** abc@xyz.com -> a***@x***.com    12345@abc.xyz.com -> 1***@a***.com */
        EMAIL,
        /** 13012345678 -> 130***5678 */
        PHONE,
        /** James -> J*    Mary -> M*    张三 -> 张* */
        NAME,
        PASSWORD,
        /** 6225807721052722 -> 62***22 */
        BANK_CARD,
        /** 000003165405049842 -> 00***42 */
        ID_CARD,
        /** 京A12345 -> 京A***45    京AA12345 -> 京A***45 */
        CAR,
        /** 北京市东城区紫禁城乾清宫 -> 北京市***乾清宫 */
        ADDRESS,
        /** 1.1.1.1 -> 1***1    192.168.0.100 -> 1***0    114.114.114.114 -> 1***4    233.5.5.5 -> 2***5 */
        IPV4,
        /** ::1 -> ***1    8f60:931a:983:390d:1b7c:9178:c6c4:d550 -> ***d550 */
        IPV6,
        /** d1651d1122894b7faf27255149c14dbc -> d16***dbc    ed3d4db1bde9a532 -> ed3***532 */
        TOKEN;

        String des(String value, boolean showLen) {
            return switch (this) {
                case EMAIL -> email(value, showLen);
                case PHONE -> U.foggyValue(value, 11, 3, 4, showLen);
                case NAME -> U.foggyValue(value, 2, 1, 0, showLen);
                case PASSWORD -> "***";
                case BANK_CARD -> U.foggyValue(value, 10, 2, 2, showLen);
                case ID_CARD -> U.foggyValue(value, 12, 2, 2, showLen);
                case CAR -> U.foggyValue(value, 7, 2, 2, showLen);
                case ADDRESS -> U.foggyValue(value, 10, 3, 3, showLen);
                case IPV4 -> U.foggyValue(value, 7, 1, 1, showLen);
                case IPV6 -> ipv6(value, showLen);
                case TOKEN -> U.foggyValue(value, 16, 3, 3, showLen);
            };
        }
        private String email(String value, boolean showLen) {
            String split = "@";
            if (value.contains(split)) {
                int index = value.indexOf(split);
                String s = U.foggyValue(value.substring(0, index), 2, 1, 0, showLen);
                String right = value.substring(index);
                String endSplit = ".";
                if (right.contains(endSplit)) {
                    int ei = right.lastIndexOf(endSplit);
                    return s + U.foggyValue(value.substring(0, ei), 2, 1, 0, showLen) + right.substring(ei);
                }
            }
            return value;
        }
        private String ipv6(String value, boolean showLen) {
            String split = ":";
            if (value.contains(split) && value.endsWith(split)) {
                int index = value.lastIndexOf(split) + 1;
                if (showLen) {
                    return "***" + (value.substring(0, index).length()) + value.substring(index);
                } else {
                    return "***" + value.substring(index);
                }
            }
            return value;
        }
    }
}
