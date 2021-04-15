package com.github.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("NullableProblems")
@RequiredArgsConstructor
@ConditionalOnClass({ HttpServletRequest.class, RequestBody.class })
@ControllerAdvice(annotations = { Controller.class, RestController.class })
public class GlobalRequestBodyAdvice extends RequestBodyAdviceAdapter {

    /** 当前端发过来的 RequestBody 数据跟相关的实体对应上时, 此时想要输出用户的输入流, 将此值设置为 true */
    @Value("${sufferErrorRequest:false}")
    private boolean sufferErrorRequest;

    private final DesensitizationParam desensitizationParam;
    private final ObjectMapper mapper;

    @Override
    public boolean supports(MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        return methodParameter.getParameterAnnotation(RequestBody.class) != null;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        if (sufferErrorRequest) {
            return new HttpInputMessage() {
                @Override
                public HttpHeaders getHeaders() {
                    return inputMessage.getHeaders();
                }

                @Override
                public InputStream getBody() throws IOException {
                    // Http Request 的 inputStream 读取过后再读取就会异常, 所以这样操作(两处都 new ByteArrayInputStream)
                    byte[] bytes = ByteStreams.toByteArray(inputMessage.getBody());

                    String data = null;
                    try (Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
                        data = CharStreams.toString(reader);
                        if (U.isNotBlank(data)) {
                            Object obj = mapper.readValue(data, Object.class);
                            LogUtil.bindRequestBody(desensitizationParam.handleDesensitization(obj));
                        }
                    } catch (Exception e) {
                        if (U.isNotBlank(data)) {
                            LogUtil.bindRequestBody(data);
                        }
                        if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                            LogUtil.ROOT_LOG.error("bind @RequestBody bytes to log-context exception", e);
                        }
                    }
                    return new ByteArrayInputStream(bytes);
                }
            };
        }
        return super.beforeBodyRead(inputMessage, parameter, targetType, converterType);
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        // body 类跟传入的 inputStream 转换失败将进不到这里面来
        if (!sufferErrorRequest) {
            LogUtil.bindRequestBody(desensitizationParam.handleDesensitization(body));
        }
        return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
    }
}
