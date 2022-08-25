package com.github.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.json.JsonUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.common.util.U;
import com.github.global.constant.GlobalConst;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("NullableProblems")
@RequiredArgsConstructor
@ConditionalOnClass({ HttpServletRequest.class, RequestBody.class })
@ControllerAdvice(annotations = { Controller.class, RestController.class })
public class RequestBodyAdvice extends RequestBodyAdviceAdapter {

    /**
     * 当前端发过来的 RequestBody 数据跟相关的实体对应不上时, 是进不到 afterBodyRead 去的
     * 想要打印入参, 将此值设置为 true(因为复制了一遍字节码, 内存消耗会比 false 时多)
     */
    @Value("${log.printComplete:true}")
    private boolean printComplete;

    @Value("${log.maxPrintLength:10000}")
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
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        if (LogUtil.ROOT_LOG.isInfoEnabled() && !GlobalConst.EXCLUDE_PATH_SET.contains(RequestUtil.getRequestUri())) {
            if (printComplete) {
                return new HttpInputMessage() {
                    @Override
                    public HttpHeaders getHeaders() {
                        return inputMessage.getHeaders();
                    }

                    @Override
                    public InputStream getBody() throws IOException {
                        // inputStream 是通过内部偏移来读取的, 读到末尾后没有指回来,
                        // 一些实现流没有实现 reset 方法, 将会报 inputStream 默认的实现 mark/reset not supported 异常
                        //   比如 tomcat 的 org.apache.catalina.connector.CoyoteInputStream
                        //   比如 undertow 的 io.undertow.servlet.spec.ServletInputStreamImpl
                        //   上面的两者都没有像 ByteArrayInputStream 实现 reset
                        // 这导致当想要重复读取时就会异常(getXX can't be called after getXXX),
                        // 所以像下面这样操作: 先读取 byte[], 输出日志后用 byte[] 再返回一个输入流
                        try (
                                InputStream input = inputMessage.getBody();
                                ByteArrayOutputStream output = new ByteArrayOutputStream()
                        ) {
                            // 用 ByteArrayOutputStream 的方式是最快的
                            // 见: https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
                            U.inputToOutput(input, output);
                            byte[] bytes = output.toByteArray();

                            String data = JsonUtil.removeWhiteSpace(new String(bytes, StandardCharsets.UTF_8));
                            String str = U.foggyValue(data, maxPrintLength, printLength, printLength);
                            LogUtil.ROOT_LOG.info("request-body({})", str);

                            return new ByteArrayInputStream(bytes);
                        }
                    }
                };
            }
        }
        return super.beforeBodyRead(inputMessage, parameter, targetType, converterType);
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (LogUtil.ROOT_LOG.isInfoEnabled() && !GlobalConst.EXCLUDE_PATH_SET.contains(RequestUtil.getRequestUri())) {
            if (!printComplete) {
                // 这样输出的内容会去除 null 值的属性
                String json = logHandler.toJson(body);
                LogUtil.ROOT_LOG.info("request-body({})", U.foggyValue(json, maxPrintLength, printLength, printLength));
            }
        }
        return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
    }
}
