package com.github.global.service;

import com.github.common.collection.MapMultiValue;
import com.github.common.collection.MultiUtil;
import com.github.common.exception.ParamException;
import com.github.common.util.A;
import com.github.common.util.U;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** 需要引入 jakarta.validation-api 包, 因此按需装载 */
@RequiredArgsConstructor
@Configuration
@ConditionalOnClass({ Validator.class })
public class ValidatorService {

    private final Validator validator;
    private final ValidationService validationService;

    /**
     * <pre>
     * 字段标下面的注解 @NotNull、@Email(groups = Xx.class) 等注解, 嵌套字段上标 @Valid(Xx.class) 注解
     *
     * 1. 自动: 在入参上标 @Validated 注解, 将抛出 MethodArgumentNotValidException 或 BindException 异常
     * 2. 半自动: 在入参上标 @Validated 注解, 用 BindingResult 做为入参, 按需调用 {@link ValidationService#validateBinding}, 抛出 ParamException 异常
     * 3. 手动: 不标 @Validated 或 @Valid 注解, 按需调用此方法, 抛出 ParamException 异常
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
     * @param groups 校验时用到的组, 见 {@link javax.validation.groups.Default}, 用任意类都行, 只要确保跟 @Null 等注解里的一致即可
     */
    public <T> void handleValidate(T obj, Class<?>... groups) {
        if (U.isNull(obj)) {
            throw new ParamException("参数不能为空");
        }
        // 这里返回的是无序的
        Set<ConstraintViolation<T>> set = validator.validate(obj, groups);
        if (A.isNotEmpty(set)) {
            Map<String, String> errorMap = validate(new LinkedHashSet<>(set));
            if (A.isNotEmpty(errorMap)) {
                throw new ParamException(errorMap);
            }
        }
    }

    public Map<String, String> validate(Set<ConstraintViolation<?>> errorSet) {
        if (A.isEmpty(errorSet)) {
            return Collections.emptyMap();
        }

        MapMultiValue<String, String, Set<String>> fieldErrorMap = MultiUtil.createLinkedMapLinkedSet();
        for (ConstraintViolation<?> error : errorSet) {
            Class<?> clazz = error.getRootBeanClass();
            String field = validationService.getParamField(clazz, error.getPropertyPath().toString());
            if (U.isNotBlank(field)) {
                fieldErrorMap.put(field, validationService.getMessage(error.getMessage()));
            }
        }
        return validationService.handleError(fieldErrorMap.asMap());
    }
}
