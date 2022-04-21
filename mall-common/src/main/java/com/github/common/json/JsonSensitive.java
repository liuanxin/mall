package com.github.common.json;

import java.lang.annotation.*;

/** 脱敏注解, 只用在 String 类型上 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonSensitive {

    /** 用在字符串类型上时要脱敏的首字符位, 比如字符串是「123456」, 当前值是 2, 则最后脱敏成「12 ***」 */
    int start();

    /** 用在字符串类型上时要脱敏的尾字符位, 比如字符串是「12345」, start 是 2, 当前值是 4, 则最后脱敏成「12 *** 56」 */
    int end();
}
