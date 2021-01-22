package com.github.global.config;

import com.github.common.json.JsonUtil;
import com.github.common.util.LogUtil;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import lombok.AllArgsConstructor;
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
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@AllArgsConstructor
@SuppressWarnings("NullableProblems")
@ConditionalOnClass({ HttpServletRequest.class, RequestBody.class })
@ControllerAdvice(annotations = { Controller.class, RestController.class })
public class GlobalRequestBodyAdvice extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        return methodParameter.getParameterAnnotation(RequestBody.class) != null;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        try {
            return new HttpInputMessage() {
                @Override
                public HttpHeaders getHeaders() {
                    return inputMessage.getHeaders();
                }
                @Override
                public InputStream getBody() throws IOException {
                    byte[] bytes = ByteStreams.toByteArray(inputMessage.getBody());
                    handleRequestBody(bytes);
                    return new ByteArrayInputStream(bytes);
                }
            };
        } catch (Exception e) {
            return inputMessage;
        }
    }
    /** 注意上下两处都是 new ByteArrayInputStream, 如果提取成变量是有问题的 */
    private static void handleRequestBody(byte[] bytes) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(bytes);
        String requestBody = CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        // 去除空白符后放到日志上下文
        LogUtil.bindRequestBody(JsonUtil.toJson(JsonUtil.toObjectNil(requestBody, Object.class)));
    }

    /*
    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        // body 类跟传入的 inputStream 转换失败将进不到这里面来, 最终将无法打印, 用上面的方式处理
        LogUtil.bindRequestBody(JsonUtil.toJson(body));
        return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
    }
    */
}
