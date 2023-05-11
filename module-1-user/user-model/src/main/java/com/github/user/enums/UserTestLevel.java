package com.github.user.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.common.util.U;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 用户等级 */
@Getter
@RequiredArgsConstructor
public enum UserTestLevel {

    Nil(0, ""), Normal(1, "普通用户"), Vip(2, "vip 用户");

    private final int code;
    private final String value;

    public static UserTestLevel fromCode(Integer code) {
        if (U.isNotNull(code)) {
            for (UserTestLevel value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
        }
        return Nil;
    }

    @JsonValue
    public int getCode() {
        return code;
    }
    /** 数据反序列化. 如 male、0、男、{"code": 0, "value": "男"} 都可以反序列化为 Gender.Male 值 */
    @JsonCreator
    public static UserTestLevel deserializer(Object obj) {
        if (U.isNotNull(obj)) {
            String str = obj.toString().trim();
            for (UserTestLevel e : values()) {
                if (str.equals(String.valueOf(e.code)) || str.equalsIgnoreCase(e.value)) {
                    return e;
                }
            }
        }
        return Nil;
    }
}
