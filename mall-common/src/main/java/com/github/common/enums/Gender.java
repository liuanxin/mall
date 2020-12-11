package com.github.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.common.util.U;
import lombok.Getter;

import java.util.Map;

/** 性别 */
@Getter
public enum Gender {

    Nil(0, "未知"), Male(1, "男"), Female(2, "女");

    @EnumValue
    int code;
    String value;
    Gender(int code, String value) {
        this.code = code;
        this.value = value;
    }

    /** 序列化给前端时, 如果只想给前端返回数值, 去掉此方法并把注解挪到 getCode 即可 */
    @JsonValue
    public Map<String, String> serializer() {
        return U.serializerEnum(code, value);
    }
    /** 数据反序列化. 如 male、0、男、{"code": 0, "value": "男"} 都可以反序列化为 Gender.Male 值 */
    @JsonCreator
    public static Gender deserializer(Object obj) {
        Gender gender = U.enumDeserializer(obj, Gender.class);
        return U.isBlank(gender) ? Nil : gender;
    }
}
