package com.github.product.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.common.util.U;

import java.util.Map;

/** 商品类型 */
public enum ProductTestType {

    Normal(0, "普通商品"), Price(1, "特价商品"), Stock(2, "特销商品");

    int code;
    String value;
    ProductTestType(int code, String value) {
        this.code = code;
        this.value = value;
    }
    /** 显示用 */
    public String getValue() {
        return value;
    }
    /** 数据关联用 */
    public int getCode() {
        return code;
    }

    /** 序列化给前端时, 如果只想给前端返回数值, 去掉此方法并把注解挪到 getCode 即可 */
    @JsonValue
    public Map<String, String> serializer() {
        return U.serializerEnum(code, value);
    }
    /** 数据反序列化. 如 male、0、男、{"code": 0, "value": "男"} 都可以反序列化为 Gender.Male 值 */
    @JsonCreator
    public static ProductTestType deserializer(Object obj) {
        ProductTestType type = U.enumDeserializer(obj, ProductTestType.class);
        return U.isBlank(type) ? Normal : type;
    }
}
