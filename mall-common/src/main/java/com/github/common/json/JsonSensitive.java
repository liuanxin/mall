package com.github.common.json;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.*;

/** 脱敏注解. 只能标在 String、Number(Integer, Long, Float, Double, BigInteger, BigDecimal)、Date 类型上 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JsonSerialize(using = JsonSensitiveSerializer.class)
@JacksonAnnotationsInside
public @interface JsonSensitive {

    /** 用在字符串类型上时要脱敏的首字符位, 比如字符串是「123456」, 当前值是 2, 则最后脱敏成「12 ***」 */
    int start() default 0;

    /** 用在字符串类型上时要脱敏的尾字符位, 比如字符串是「123456」, start 是 2, 当前值是 2, 则最后脱敏成「12 *** 56」 */
    int end() default 0;

    /** 用在 Number 类型上时的随机数, 比如当前值是 100, 则最后脱敏成 1 ~ 100 之间的数, <= 0 则表示使用原值 */
    double randomNumber() default 0D;

    /** 用在 Number 类型上, 如果是浮点数, 保留的小数位 */
    int digitsNumber() default 2;

    /** 用在 Date 类型上时的随机数, 比如日期是「2022-01-01」, 当前值是 1234567, 则最后脱敏成「2020-01-01 - (1 ~ 1234567)」, <= 0 则表示使用原值 */
    long randomDate() default 0;
}
