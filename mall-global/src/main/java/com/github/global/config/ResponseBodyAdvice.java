package com.github.global.config;

import com.github.common.date.Dates;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.common.util.U;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

import java.lang.reflect.Method;
import java.util.List;

@SuppressWarnings("NullableProblems")
@RequiredArgsConstructor
@ConditionalOnClass({ ResponseBody.class })
@ControllerAdvice(annotations = { Controller.class, RestController.class })
public class ResponseBodyAdvice extends AbstractMappingJacksonResponseBodyAdvice {

    @Value("${req.log-exclude-path:}")
    private List<String> excludePathList;

    private final GlobalLogHandler logHandler;

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType, MethodParameter parameter,
                                           ServerHttpRequest request, ServerHttpResponse response) {
        if (A.isEmpty(excludePathList) || !excludePathList.contains(RequestUtil.getRequestUri())) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                StringBuilder sbd = new StringBuilder();
                ServletServerHttpRequest req = (ServletServerHttpRequest) request;
                sbd.append("[").append(req.getMethod()).append(" ").append(req.getURI()).append("]");
                long startTime = LogUtil.getStartTime();
                if (U.greater0(startTime)) {
                    sbd.append(" time(").append(Dates.toHuman(System.currentTimeMillis() - startTime)).append(") ");
                }

                sbd.append(parameter.getContainingClass().getName());
                Method method = parameter.getMethod();
                if (U.isNotNull(method)) {
                    sbd.append("#").append(method.getName());
                }

                sbd.append(" return(").append(logHandler.toJson(bodyContainer.getValue())).append(")");
                LogUtil.ROOT_LOG.info(sbd.toString());
            }
        }
    }
}
