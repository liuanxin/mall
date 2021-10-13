package com.github.global.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.common.exception.ParamException;
import com.github.common.util.A;
import com.github.common.util.U;
import com.google.common.collect.Maps;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.Map;

public class ValidationUtil {

    public static void handleValidate(BindingResult bindingResult) {
        Map<String, String> fieldErrorMap = validate(bindingResult);
        if (A.isNotEmpty(fieldErrorMap)) {
            throw new ParamException(fieldErrorMap);
        }
    }

    public static Map<String, String> validate(BindingResult bindingResult) {
        Object obj = bindingResult.getTarget();
        Class<?> clazz = U.isNull(obj) ? null : obj.getClass();

        Map<String, String> fieldErrorMap = Maps.newLinkedHashMap();
        for (ObjectError error : bindingResult.getAllErrors()) {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                fieldErrorMap.put(getParamField(clazz, fieldError.getField()), fieldError.getDefaultMessage());
            }
        }
        return fieldErrorMap;
    }

    static String getParamField(Class<?> clazz, String field) {
        if (U.isNotNull(clazz) && U.isNotBlank(field)) {
            try {
                JsonProperty property = AnnotationUtils.findAnnotation(clazz.getDeclaredField(field), JsonProperty.class);
                // 属性上如果没有标 @JsonProperty 注解就返回属性名
                return U.isNull(property) ? field : property.value();
            } catch (Exception ignore) {
            }
        }
        return field;
    }
}
