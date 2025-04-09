package com.github.global.config;

import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.common.util.U;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

@SuppressWarnings("NullableProblems")
@RequiredArgsConstructor
@ConditionalOnClass({ RequestBody.class })
@ControllerAdvice(annotations = { Controller.class, RestController.class })
public class RequestBodyAdvice extends RequestBodyAdviceAdapter {

    @Value("${req.log-exclude-path:}")
    private List<String> excludePathList;

    private final GlobalLogHandler logHandler;

    @Override
    public boolean supports(MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        return methodParameter.hasParameterAnnotation(RequestBody.class);
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        if (A.isEmpty(excludePathList) || !excludePathList.contains(RequestUtil.getRequestUri())) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                return new HttpInputMessage() {
                    @Override
                    public HttpHeaders getHeaders() {
                        return inputMessage.getHeaders();
                    }

                    @Override
                    public InputStream getBody() throws IOException {
                        // inputStream 是通过内部偏移来读取的, 读到末尾后没有指回来,
                        // 一些实现流没有实现 reset 方法, 将会报 inputStream 默认的实现 mark/reset not supported 异常, 比如
                        //   tomcat 的 org.apache.catalina.connector.CoyoteInputStream
                        //   undertow 的 io.undertow.servlet.spec.ServletInputStreamImpl
                        //   jetty 的 org.eclipse.jetty.server.HttpInputOverHTTP
                        // 这导致当想要重复读取时会报 getXX can't be called after getXXX 异常,
                        // 所以像下面这样操作: 先将流读取成 byte[], 输出日志后用 byte[] 再返回一个输入流
                        try (
                                InputStream input = inputMessage.getBody();
                                ByteArrayOutputStream output = new ByteArrayOutputStream()
                        ) {
                            // 用 ByteArrayOutputStream 的方式是最快的
                            // 见: https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
                            U.inputToOutput(input, output);
                            byte[] bytes = output.toByteArray();

                            String originalBody = new String(bytes, StandardCharsets.UTF_8);
                            // 这里的目的是为了打印日志, 先转成 object 再 toJson 有两个目的:
                            // 1.去掉原数据中可能有的空白符(换行制表空格等), 2.字段脱敏(密码字段的值输出成 *** 等)
                            String body = logHandler.toJson(JsonUtil.toObjectNil(originalBody, Object.class));

                            String method = RequestUtil.getMethod();
                            String url = RequestUtil.getRequestUrl();
                            LogUtil.ROOT_LOG.info("[{} {}] request-body({})", method, url, U.defaultIfBlank(body, originalBody));

                            // 在 ByteArrayOutputStream 和 ByteArrayInputStream 上调用 close 是无意义的, 它们也都有实现 reset 方法
                            return new ByteArrayInputStream(bytes);
                        }
                    }
                };
            }
        }
        return super.beforeBodyRead(inputMessage, parameter, targetType, converterType);
    }
}
