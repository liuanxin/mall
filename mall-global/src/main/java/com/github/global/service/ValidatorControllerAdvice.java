package com.github.global.service;

import com.github.common.json.JsonCode;
import com.github.common.json.JsonResult;
import com.github.common.json.JsonUtil;
import com.github.common.util.LogUtil;
import com.google.common.base.Joiner;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Map;

/**
 * 需要引入 jakarta.validation-api 包, 因此按需装载
 *
 * @see org.hibernate.validator.HibernateValidator
 * @see org.springframework.validation.beanvalidation.MethodValidationPostProcessor
 * @see org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration
 */
@RestControllerAdvice
@RequiredArgsConstructor
@ConditionalOnClass({ Validator.class })
public class ValidatorControllerAdvice {

    @Value("${res.returnStatusCode:false}")
    private boolean returnStatusCode;

    private final ValidatorService validatorService;

    /** 使用 validation(比如 hibernate entity manager)时, 验证不通过抛出的异常 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<JsonResult<String>> paramValidException(ConstraintViolationException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("constraint violation fails", e);
        }
        Map<String, String> errorMap = validatorService.handleValidate(e.getConstraintViolations());
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        JsonResult<String> result = JsonResult.badRequest(Joiner.on(",").join(errorMap.values()), errorMap);
        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            LogUtil.ROOT_LOG.info("Constraint validator exception result: ({})", JsonUtil.toJson(result), e);
        }
        return ResponseEntity.status(status).body(result);
    }
}
