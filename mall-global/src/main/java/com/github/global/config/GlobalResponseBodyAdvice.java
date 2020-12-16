package com.github.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.date.DateUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtils;
import com.github.common.util.U;
import javassist.ClassPool;
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
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType, MethodParameter parameter,
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

                    Class<?> clazz = parameter.getContainingClass();
                    String className = clazz.getName();
                    Method method = parameter.getMethod();
                    String methodName = U.isNotBlank(method) ? method.getName() : U.EMPTY;
                    int line;
                    try {
                        line = ClassPool.getDefault().get(className).getDeclaredMethod(methodName).getMethodInfo().getLineNumber(0);
                    } catch (Exception e) {
                        line = 0;
                    }

                    StringBuilder sbd = new StringBuilder();
                    sbd.append(className);
                    if (U.isNotBlank(methodName)) {
                        sbd.append("#").append(methodName);
                    }
                    sbd.append("(").append(clazz.getSimpleName()).append(".java");
                    if (U.greater0(line)) {
                        sbd.append(":").append(line);
                    }
                    sbd.append(")");

                    sbd.append(", time: (");
                    sbd.append(DateUtil.toHuman(System.currentTimeMillis() - LogUtil.getStartTimeMillis()));
                    sbd.append(")");

                    sbd.append(", return: (");
                    int max = 1000, printLen = 200;
                    int len = json.length();
                    if (online && len >= max) {
                        sbd.append(json, 0, printLen).append(" ... ").append(json, len - printLen, len);
                    } else {
                        sbd.append(json);
                    }
                    sbd.append(")");

                    LogUtil.ROOT_LOG.info(sbd.toString());
                } finally {
                    if (notRequestInfo) {
                        LogUtil.unbind();
                    }
                }
            }
        }
    }
}
