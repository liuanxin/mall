package com.github.global.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.common.exception.ParamException;
import com.github.common.util.A;
import com.github.common.util.U;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.lang.reflect.Field;
import java.util.*;

@RequiredArgsConstructor
@Configuration
public class ValidationService {

    private final I18nService i18nService;

    /**
     * <pre>
     * 字段标下面的注解 @NotNull、@Email(groups = Xx.class) 等注解, 嵌套字段上标 @Valid 注解
     *
     * 1. 自动验证: 在方法参数上标 @Validated(Xx.class) 注解, 将抛出 MethodArgumentNotValidException 或 BindException 异常
     * 2. 半自动验证: 在方法参数上标 @Validated(Xx.class) 注解, 用 BindingResult 做为入参, 调用此方法, 抛出 ParamException 异常
     * 3. 手动验证: 不标 @Validated 或 @Valid 注解
     * </pre>
     *
     * @see javax.validation.constraints.Null
     * @see javax.validation.constraints.NotNull
     * @see javax.validation.constraints.NotEmpty
     * @see javax.validation.constraints.NotBlank
     * @see javax.validation.constraints.Email
     * @see javax.validation.constraints.Min
     * @see javax.validation.constraints.Max
     * @see javax.validation.constraints.Pattern
     */
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
            String field = getParamField(clazz, error.getField());
            if (U.isNotBlank(field)) {
                fieldErrorMap.put(field, getMessage(error.getDefaultMessage()));
            }
        }
        return handleError(fieldErrorMap.asMap());
    }

    private String getParamField(Class<?> clazz, String field) {
        if (U.isNull(clazz)) {
            return field;
        } else {
            List<String> modelList = new ArrayList<>();
            calcParamProperty(clazz, field, modelList);
            return Joiner.on(".").join(modelList);
        }
    }

    /** 字段上如果标了 @JsonProperty 则使用注解值返回 */
    private void calcParamProperty(Class<?> clazz, String field, List<String> modelList) {
        if (field.contains(".")) {
            int index = field.indexOf(".");
            String first = field.substring(0, index);
            Field fd = U.getField(clazz, first);
            if (U.isNotNull(fd)) {
                JsonProperty property = AnnotationUtils.findAnnotation(fd, JsonProperty.class);
                modelList.add(U.callIfNotNull(property, JsonProperty::value, first));

                String second = field.substring(index + 1);
                calcParamProperty(fd.getType(), second, modelList);
            }
        } else {
            Field fd = U.getField(clazz, field);
            JsonProperty property = AnnotationUtils.findAnnotation(fd, JsonProperty.class);
            modelList.add(U.callIfNotNull(property, JsonProperty::value, field));
        }
    }

    private String getValue(Class<?> clazz, String field) {
        Field fd = U.getField(clazz, field);
        if (U.isNull(fd)) {
            return U.EMPTY;
        } else {
            JsonProperty property = AnnotationUtils.findAnnotation(fd, JsonProperty.class);
            return U.callIfNotNull(property, JsonProperty::value, field);
        }
    }

    /** 如果值是以 { 开头且以 } 结尾则调用 i18n 处理国际化 */
    public String getMessage(String msg) {
        if (U.isBlank(msg)) {
            return msg;
        }

        String trim = msg.trim();
        if (trim.startsWith("{") && trim.endsWith("}")) {
            return i18nService.getMessage(trim.substring(1, trim.length() - 1));
        } else {
            return msg;
        }
    }

    public Map<String, String> handleError(Map<String, Collection<String>> fieldErrorMap) {
        Map<String, String> errorMap = new LinkedHashMap<>();
        if (A.isNotEmpty(fieldErrorMap)) {
            for (Map.Entry<String, Collection<String>> entry : fieldErrorMap.entrySet()) {
                errorMap.put(entry.getKey(), A.toStr(entry.getValue()));
            }
        }
        return errorMap;
    }
}
