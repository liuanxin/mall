package com.github.product.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.common.util.U;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/** 商品类型 */
@Getter
@RequiredArgsConstructor
public enum ProductTestType {

    Nil(0, ""), Normal(1, "普通商品"), Price(2, "特价商品"), Stock(3, "特销商品");

    @EnumValue
    private final int code;
    private final String value;

    public static ProductTestType fromCode(Integer code) {
        if (U.isNotNull(code)) {
            for (ProductTestType value : values()) {
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
    public static ProductTestType deserializer(Object obj) {
        ProductTestType type = U.enumDeserializer(obj, ProductTestType.class);
        return U.isNull(type) ? Normal : type;
    }
}
