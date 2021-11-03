package com.github.global.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.common.util.U;
import com.github.global.constant.GlobalConst;
import com.google.common.io.ByteStreams;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("NullableProblems")
@RequiredArgsConstructor
@ConditionalOnClass({ HttpServletRequest.class, RequestBody.class })
@ControllerAdvice(annotations = { Controller.class, RestController.class })
public class GlobalRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private final JsonDesensitization jsonDesensitization;
    private final ObjectMapper mapper;

    @Override
    public boolean supports(MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        return methodParameter.getParameterAnnotation(RequestBody.class) != null;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        if (!GlobalConst.EXCLUDE_PATH_SET.contains(RequestUtil.getRequestUri()) && LogUtil.ROOT_LOG.isInfoEnabled()) {
            return new HttpInputMessage() {
                @Override
                public HttpHeaders getHeaders() {
                    return inputMessage.getHeaders();
                }

                @Override
                public InputStream getBody() throws IOException {
                    // Http Request 的 inputStream 读取过后再读取就会异常, 所以这样操作(两处都 new ByteArrayInputStream)
                    byte[] bytes = ByteStreams.toByteArray(inputMessage.getBody());

                    String data = new String(bytes, StandardCharsets.UTF_8);
                    if (U.isNotBlank(data)) {
                        try {
                            // 先转换成对象, 再输出成 string, 这样可以去掉空白符
                            Object obj = mapper.readValue(data, Object.class);
                            LogUtil.ROOT_LOG.info("before body({})", jsonDesensitization.toJson(obj));
                        } catch (JsonProcessingException e) {
                            LogUtil.ROOT_LOG.error(String.format("@RequestBody(%s) has not json data", data), e);
                        }
                    }
                    return new ByteArrayInputStream(bytes);
                }
            };
        } else {
            return super.beforeBodyRead(inputMessage, parameter, targetType, converterType);
        }
    }
}
