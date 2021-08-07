package com.github.global.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.common.util.U;
import com.github.global.constant.GlobalConst;
import com.google.common.io.ByteStreams;
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
public class GlobalRequestBodyAdvice extends RequestBodyAdviceAdapter {

    /** 当前端发过来的 RequestBody 数据跟相关的实体对应上时, 此时想要输出用户的输入流, 将此值设置为 true(因为复制了一遍字节码, 内存消耗会比 false 时多) */
    @Value("${json.sufferErrorRequest:false}")
    private boolean sufferErrorRequest;

    @Value("${json.logPrintHeader:false}")
    private boolean printHeader;

    private final JsonDesensitization jsonDesensitization;
    private final ObjectMapper mapper;

    @Override
    public boolean supports(MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        return methodParameter.getParameterAnnotation(RequestBody.class) != null;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        if (!GlobalConst.EXCLUDE_PATH_SET.contains(RequestUtil.getRequestUri())) {
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

                        String data = new String(bytes, StandardCharsets.UTF_8);
                        if (U.isNotBlank(data)) {
                            try {
                                // 先转换成对象, 再输出成 string, 这样可以去掉空白符
                                Object obj = mapper.readValue(data, Object.class);
                                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                                    LogUtil.ROOT_LOG.info("before body({})", jsonDesensitization.toJson(obj));
                                }
                            } catch (JsonProcessingException e) {
                                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                                    LogUtil.ROOT_LOG.error(String.format("@RequestBody(%s) has not json data", data), e);
                                }
                            }
                        }
                        return new ByteArrayInputStream(bytes);
                    }
                };
            }
        }
        return super.beforeBodyRead(inputMessage, parameter, targetType, converterType);
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (!GlobalConst.EXCLUDE_PATH_SET.contains(RequestUtil.getRequestUri())) {
            if (!sufferErrorRequest) {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("after body({})", jsonDesensitization.toJson(body));
                }
            }
        }
        return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
    }

    private String formatHeader() {
        return printHeader ? String.format(" header(%s)", RequestUtil.formatHeader()) : U.EMPTY;
    }
}
