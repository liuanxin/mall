package com.github.global.service;

import com.github.common.exception.ParamException;
import com.github.common.util.A;
import com.github.common.util.U;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
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
     * 字段标下面的注解 @NotNull、@Email(groups = Xx.class) 等注解, 嵌套字段上标 @Valid 注解
     *
     * 1. 自动验证: 在方法参数上标 @Validated(Xx.class) 注解, 将抛出 MethodArgumentNotValidException 或 BindException 异常
     * 2. 半自动验证: 在方法参数上标 @Validated(Xx.class) 注解, 用 BindingResult 做为入参
     * 3. 手动验证: 不标 @Validated 或 @Valid 注解, 调用此方法, 抛出 ParamException 异常
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
    public void handleValidate(Object obj, Class<?>... groups) {
        if (U.isNull(obj)) {
            throw new ParamException("参数不能为空");
        }
        Set<ConstraintViolation<Object>> set = validator.validate(obj, groups);
        if (A.isNotEmpty(set)) {
            Map<String, String> errorMap = handleValidate(new LinkedHashSet<>(set));
            if (A.isNotEmpty(errorMap)) {
                throw new ParamException(errorMap);
            }
        }
    }

    public Map<String, String> handleValidate(Set<ConstraintViolation<?>> errorSet) {
        if (A.isEmpty(errorSet)) {
            return Collections.emptyMap();
        } else {
            Multimap<String, String> fieldErrorMap = ArrayListMultimap.create();
            for (ConstraintViolation<?> error : errorSet) {
                String field = error.getPropertyPath().toString();
                fieldErrorMap.put(field, validationService.getMessage(error.getMessage()));
            }
            return validationService.handleError(fieldErrorMap.asMap());
        }
    }
}
