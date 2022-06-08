package com.github.global.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.common.exception.ParamException;
import com.github.common.util.A;
import com.github.common.util.U;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.*;

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

        Multimap<String, String> fieldErrorMap = ArrayListMultimap.create();
        for (ObjectError error : bindingResult.getAllErrors()) {
            if (error instanceof FieldError fieldError) {
                fieldErrorMap.put(getParamField(clazz, fieldError.getField()), fieldError.getDefaultMessage());
            }
        }
        return handleError(fieldErrorMap.asMap());
    }

    static Map<String, String> handleError(Map<String, Collection<String>> fieldErrorMap) {
        Map<String, String> errorMap = new LinkedHashMap<>();
        if (A.isNotEmpty(fieldErrorMap)) {
            for (Map.Entry<String, Collection<String>> entry : fieldErrorMap.entrySet()) {
                List<String> list = new ArrayList<>(entry.getValue());
                list.sort(null);
                errorMap.put(entry.getKey(), A.toStr(list));
            }
        }
        return errorMap;
    }

    static String getParamField(Class<?> clazz, String field) {
        JsonProperty property = AnnotationUtils.findAnnotation(U.getField(clazz, field), JsonProperty.class);
        // 属性上如果没有标 @JsonProperty 注解就返回属性名
        return U.isNull(property) ? field : property.value();
    }
}
