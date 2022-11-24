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

    boolean showLen() default false;


    enum Strategy {
        USER_NAME, PASSWORD, PHONE, ADDRESS, ID_CARD, TOKEN;

        String des(Object obj, boolean showLen) {
            if (U.isNull(obj)) {
                return null;
            }

            String value = obj.toString();
            return switch (this) {
                case USER_NAME -> U.foggyValue(value, 2, 1, 0, showLen);
                case PASSWORD -> "***";
                case PHONE -> U.foggyValue(value, 11, 3, 4, showLen);
                case ADDRESS -> U.foggyValue(value, 10, 3, 3, showLen);
                case ID_CARD -> U.foggyValue(value, 15, 2, 2, showLen);
                case TOKEN -> U.foggyValue(value, 16, 3, 3, showLen);
            };
        }
    }
}
