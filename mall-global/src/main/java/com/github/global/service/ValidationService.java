package com.github.global.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.common.exception.ParamException;
import com.github.common.util.A;
import com.github.common.util.U;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.*;

@RequiredArgsConstructor
@Configuration
public class ValidationService {

    private final I18nService i18nService;

    public void handleValidate(BindingResult bindingResult) {
        Map<String, String> fieldErrorMap = validate(bindingResult);
        if (A.isNotEmpty(fieldErrorMap)) {
            throw new ParamException(fieldErrorMap);
        }
    }

    public Map<String, String> validate(BindingResult bindingResult) {
        Object obj = bindingResult.getTarget();
        Class<?> clazz = U.isNull(obj) ? null : obj.getClass();

        Multimap<String, String> fieldErrorMap = ArrayListMultimap.create();
        for (FieldError error : bindingResult.getFieldErrors()) {
            fieldErrorMap.put(getParamField(clazz, error.getField()), getMessage(error.getDefaultMessage()));
        }
        return handleError(fieldErrorMap.asMap());
    }

    public Map<String, String> handleError(Map<String, Collection<String>> fieldErrorMap) {
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

    /** 字段上如果标了 @JsonProperty 则使用注解值返回 */
    public String getParamField(Class<?> clazz, String field) {
        if (U.isNull(clazz)) {
            return field;
        } else {
            JsonProperty property = AnnotationUtils.findAnnotation(U.getField(clazz, field), JsonProperty.class);
            return U.callIfNotNull(property, JsonProperty::value, field);
        }
    }

    /** 如果值是以 { 开头且以 } 结尾则调用 i18n 处理国际化 */
    public String getMessage(String msg) {
        if (U.isNotBlank(msg) && msg.startsWith("{") && msg.endsWith("}")) {
            msg = msg.substring(1, msg.length() - 1);
            return i18nService.getMessage(msg);
        } else {
            return msg;
        }
    }
}
