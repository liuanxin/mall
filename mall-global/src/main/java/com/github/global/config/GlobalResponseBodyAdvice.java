package com.github.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtils;
import com.github.common.util.U;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@SuppressWarnings({"NullableProblems"})
@ConditionalOnClass({ HttpServletResponse.class, ResponseBody.class })
@ControllerAdvice(annotations = { Controller.class, RestController.class })
public class GlobalResponseBodyAdvice extends AbstractMappingJacksonResponseBodyAdvice {

    @Value("${online:false}")
    private boolean online;

    private final ObjectMapper objectMapper;
    public GlobalResponseBodyAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (!super.supports(returnType, converterType)) {
            return false;
        }
        Method method = returnType.getMethod();
        if (method != null) {
            if (method.isAnnotationPresent(ResponseBody.class)) {
                return true;
            }
            Class<?> type = method.getDeclaringClass();
            return type.isAnnotationPresent(ResponseBody.class) || type.isAnnotationPresent(RestController.class);
        }
        return false;
    }

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType, MethodParameter returnType,
                                           ServerHttpRequest request, ServerHttpResponse response) {
        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            String json;
            try {
                json = objectMapper.writeValueAsString(bodyContainer.getValue());
            } catch (Exception ignore) {
                return;
            }

            if (U.isNotBlank(json)) {
                boolean notRequestInfo = LogUtil.hasNotRequestInfo();
                try {
                    if (notRequestInfo) {
                        LogUtil.bindContext(RequestUtils.logContextInfo());
                    }
                    long time = System.currentTimeMillis() - LogUtil.getStartTimeMillis();
                    // 如果在生产环境, 太长就只输出前后, 不全部输出
                    String print = online ? U.toStr(json, 1000, 200) : json;
                    LogUtil.ROOT_LOG.info("use time ({}ms), return: ({})", time, print);
                } finally {
                    if (notRequestInfo) {
                        LogUtil.unbind();
                    }
                }
            }
        }
    }
}
