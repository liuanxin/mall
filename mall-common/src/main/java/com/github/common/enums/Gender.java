package com.github.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.common.util.U;

/** 性别 */
public enum Gender {

    Nil(0, ""), Male(1, "男"), Female(2, "女");

    private final int code;
    private final String value;
    Gender(int code, String value) {
        this.code = code;
        this.value = value;
    }

    @JsonValue
    public int getCode() {
        return code;
    }
    public String getValue() {
        return value;
    }

    public static Gender fromCode(Integer code) {
        if (U.isNotNull(code)) {
            for (Gender value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
        }
        return Nil;
    }

    /** 数据反序列化. 如 male、0、男、{"code": 0, "value": "男"} 都可以反序列化为 Gender.Male 值 */
    @JsonCreator
    public static Gender deserializer(Object obj) {
        if (U.isNotNull(obj)) {
            String str = obj.toString().trim();
            for (Gender e : values()) {
                if (str.equals(String.valueOf(e.code)) || str.equalsIgnoreCase(e.value)) {
                    return e;
                }
            }
        }
        return Nil;
    }

    /** 基于身份证号码返回性别 */
    public static Gender fromIdCard(String num) {
        String idCard = num.trim();
        if (U.hasIdCard(idCard)) {
            String gender = switch (idCard.length()) {
                case 15 -> idCard.substring(14, 15);
                case 18 -> idCard.substring(16, 17);
                default -> U.EMPTY;
            };
            return (U.isNotBlank(gender) && U.isInt(gender) && U.toInt(gender) % 2 == 1) ? Gender.Male : Gender.Female;
        } else {
            return Gender.Nil;
        }
    }
}
