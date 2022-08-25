package com.github.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.common.util.U;
import com.github.global.constant.GlobalConst;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;

@SuppressWarnings("NullableProblems")
@RequiredArgsConstructor
@ConditionalOnClass({ HttpServletRequest.class, RequestBody.class })
@ControllerAdvice(annotations = { Controller.class, RestController.class })
public class RequestBodyAdvice extends RequestBodyAdviceAdapter {

    @Value("${log.maxPrintLength:50000}")
    private int maxPrintLength;
    @Value("${log.printLength:1000}")
    private int printLength;

    private final GlobalLogHandler logHandler;
    private final ObjectMapper mapper;

    @Override
    public boolean supports(MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        return methodParameter.getParameterAnnotation(RequestBody.class) != null;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (LogUtil.ROOT_LOG.isInfoEnabled() && !GlobalConst.EXCLUDE_PATH_SET.contains(RequestUtil.getRequestUri())) {
            String json = logHandler.toJson(body);
            String str = U.foggyValue(json, maxPrintLength, printLength, printLength);
            LogUtil.ROOT_LOG.info("RequestBody({})", str);
        }
        return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
    }
}
