package com.github.global.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.common.exception.ParamException;
import com.github.common.util.A;
import com.github.common.util.U;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

        Multimap<String, String> fieldErrorMap = ArrayListMultimap.create();
        for (ObjectError error : bindingResult.getAllErrors()) {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                fieldErrorMap.put(getParamField(clazz, fieldError.getField()), fieldError.getDefaultMessage());
            }
        }
        return handleError(fieldErrorMap);
    }

    static Map<String, String> handleError(Multimap<String, String> fieldErrorMap) {
        Map<String, String> errorMap = Maps.newLinkedHashMap();
        if (!fieldErrorMap.isEmpty()) {
            for (Map.Entry<String, Collection<String>> entry : fieldErrorMap.asMap().entrySet()) {
                List<String> list = Lists.newArrayList(entry.getValue());
                Collections.sort(list);
                errorMap.put(entry.getKey(), Joiner.on(",").join(list));
            }
        }
        return errorMap;
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
