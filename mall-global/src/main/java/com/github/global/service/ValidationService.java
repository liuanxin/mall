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
import java.lang.reflect.ParameterizedType;
import java.util.*;

@RequiredArgsConstructor
@Configuration
public class ValidationService {

    private final I18nService i18nService;

    /**
     * <pre>
     * 字段标下面的注解 @NotNull、@Email(groups = Xx.class) 等注解, 嵌套字段上标 @Valid(Xx.class) 注解
     *
     * 1. 自动: 在入参上标 @Validated 注解, 将抛出 MethodArgumentNotValidException 或 BindException 异常
     * 2. 半自动: 在入参上标 @Validated 注解, 用 BindingResult 做为入参, 见 {@link ValidatorService#handleValidate}
     * 3. 手动: 不标 @Validated 或 @Valid 注解, 调用此方法, 抛出 ParamException 异常
     * </pre>
     *
     * @see com.github.global.config.GlobalException#paramValidException
     * @see javax.validation.constraints.Null
     * @see javax.validation.constraints.NotNull
     * @see javax.validation.constraints.NotEmpty
     * @see javax.validation.constraints.NotBlank
     * @see javax.validation.constraints.Email
     * @see javax.validation.constraints.Min
     * @see javax.validation.constraints.Max
     * @see javax.validation.constraints.Pattern
     */
    public void handleValidate(BindingResult result) {
        Map<String, String> fieldErrorMap = validate(result);
        if (A.isNotEmpty(fieldErrorMap)) {
            throw new ParamException(fieldErrorMap);
        }
    }

    public Map<String, String> validate(BindingResult result) {
        Object obj = result.getTarget();
        Class<?> clazz = U.isNull(obj) ? null : obj.getClass();

        Multimap<String, String> fieldErrorMap = ArrayListMultimap.create();
        List<FieldError> errors = result.getFieldErrors();
        for (FieldError error : errors) {
            String field = getParamField(clazz, error.getField());
            if (U.isNotBlank(field)) {
                fieldErrorMap.put(field, getMessage(error.getDefaultMessage()));
            }
        }
        return handleError(fieldErrorMap.asMap());
    }

    public String getParamField(Class<?> clazz, String field) {
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
            String model, suffix;
            if (first.contains("[")) {
                int idx = first.indexOf("[");
                model = first.substring(0, idx);
                suffix = first.substring(idx);
            } else {
                model = first;
                suffix = U.EMPTY;
            }
            Field fd = U.getField(clazz, model);
            if (U.isNotNull(fd)) {
                JsonProperty property = AnnotationUtils.findAnnotation(fd, JsonProperty.class);
                modelList.add(U.callIfNotNull(property, JsonProperty::value, first) + suffix);

                // 只处理数组 model[0].xxx 和 键值对 model[xx].xxx 的情况, 其他的泛型无法处理
                Class<?> type;
                String typeName = fd.getType().getTypeName();
                if (typeName.equals(List.class.getName())) {
                    type = (Class<?>) ((ParameterizedType) fd.getGenericType()).getActualTypeArguments()[0];
                } else if (typeName.equals(Map.class.getName())) {
                    type = (Class<?>) ((ParameterizedType) fd.getGenericType()).getActualTypeArguments()[1];
                } else {
                    type = fd.getType();
                }
                calcParamProperty(type, field.substring(index + 1), modelList);
            }
        } else {
            Field fd = U.getField(clazz, field);
            if (U.isNotNull(fd)) {
                JsonProperty property = AnnotationUtils.findAnnotation(fd, JsonProperty.class);
                modelList.add(U.callIfNotNull(property, JsonProperty::value, field));
            }
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
        // 返回是无序的, 这里用 key 排序
        Map<String, String> errorMap = new TreeMap<>();
        if (A.isNotEmpty(fieldErrorMap)) {
            for (Map.Entry<String, Collection<String>> entry : fieldErrorMap.entrySet()) {
                errorMap.put(entry.getKey(), A.toStr(entry.getValue()));
            }
        }
        return errorMap;
    }
}
