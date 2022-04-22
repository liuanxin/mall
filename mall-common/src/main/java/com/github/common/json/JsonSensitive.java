package com.github.common.json;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.*;

/**
 * 脱敏注解, 只能标在 String 类型上, 标在 非 String 类型上时将会
 * Caused by: java.lang.ClassCastException: class XXX cannot be cast to class java.lang.String
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JsonSerialize(using = JsonSensitiveSerializer.class)
@JacksonAnnotationsInside
public @interface JsonSensitive {

    /** 用在字符串类型上时要脱敏的首字符位, 比如字符串是「123456」, 当前值是 2, 则最后脱敏成「12 ***」 */
    int start();

    /** 用在字符串类型上时要脱敏的尾字符位, 比如字符串是「123456」, start 是 2, 当前值是 2, 则最后脱敏成「12 *** 56」 */
    int end();
}
