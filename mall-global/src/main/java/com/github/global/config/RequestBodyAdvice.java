package com.github.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.util.A;
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
    @Value("${log.printComplete:false}")
    private boolean printComplete;

    @Value("${log.maxPrintLength:200000}")
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
                        // Http Request 的 inputStream 读取过后再读取就会异常, 所以这样操作(两处都 new ByteArrayInputStream)
                        try (InputStream inputStream = inputMessage.getBody()) {
                            byte[] bytes = inputStream.readAllBytes();
                            if (A.isNotEmptyObj(bytes)) {
                                // 这样输出的内容可能会有很多空白符(空格, 换行等)
                                String data = new String(bytes, StandardCharsets.UTF_8);
                                LogUtil.ROOT_LOG.info("RequestBody({})", U.toStr(data, maxPrintLength, printLength));
                            }
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
                LogUtil.ROOT_LOG.info("RequestBody({})", U.toStr(json, maxPrintLength, printLength));
            }
        }
        return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
    }
}
