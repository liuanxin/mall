package com.github.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.common.util.U;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/** 用户等级 */
@Getter
@RequiredArgsConstructor
public enum UserTestLevel {

    Nil(0, ""), Normal(1, "普通用户"), Vip(2, "vip 用户");

    @EnumValue
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

    /** 序列化给前端时, 如果只想给前端返回数值, 去掉此方法并把注解挪到 getCode 即可 */
    @JsonValue
    public Map<String, Object> serializer() {
        return U.serializerEnum(code, value);
    }
    /** 数据反序列化. 如 male、0、男、{"code": 0, "value": "男"} 都可以反序列化为 Gender.Male 值 */
    @JsonCreator
    public static UserTestLevel deserializer(Object obj) {
        UserTestLevel level = U.enumDeserializer(obj, UserTestLevel.class);
        return U.isNull(level) ? Normal : level;
    }
}
