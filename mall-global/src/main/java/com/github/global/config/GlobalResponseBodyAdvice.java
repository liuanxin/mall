package com.github.global.config;

import com.github.common.Const;
import com.github.common.date.DateUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtils;
import com.github.common.util.U;
import com.github.global.constant.GlobalConst;
import javassist.ClassPool;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
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
import java.util.Date;

@SuppressWarnings({"NullableProblems"})
@RequiredArgsConstructor
@ConditionalOnClass({ HttpServletResponse.class, ResponseBody.class })
@ControllerAdvice(annotations = { Controller.class, RestController.class })
public class GlobalResponseBodyAdvice extends AbstractMappingJacksonResponseBodyAdvice {

    @Value("${online:false}")
    private boolean online;

    private final JsonDesensitization jsonDesensitization;

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType, MethodParameter parameter,
                                           ServerHttpRequest request, ServerHttpResponse response) {
        String uri = RequestUtils.getRequestUri();
        if (LogUtil.ROOT_LOG.isInfoEnabled() && !GlobalConst.EXCLUDE_PATH_SET.contains(uri)) {
            String json = jsonDesensitization.handle(bodyContainer.getValue());
            if (U.isNotBlank(json)) {
                boolean notRequestInfo = LogUtil.hasNotRequestInfo();
                try {
                    if (notRequestInfo) {
                        String traceId = RequestUtils.getCookieOrHeaderOrParam(Const.TRACE);
                        LogUtil.bindContext(traceId, RequestUtils.logContextInfo());
                    }

                    Class<?> clazz = parameter.getContainingClass();
                    String className = clazz.getName();
                    Method method = parameter.getMethod();
                    String methodName = U.isNotBlank(method) ? method.getName() : U.EMPTY;

                    StringBuilder sbd = new StringBuilder();
                    sbd.append(className);
                    if (U.isNotBlank(methodName)) {
                        sbd.append("#").append(methodName);
                    }
                    try {
                        ClassPool classPool = new ClassPool(ClassPool.getDefault());
                        String classInFile = U.getClassInFile(clazz);
                        if (U.isNotBlank(classInFile)) {
                            classPool.appendClassPath(classInFile);
                        }
                        int line = classPool.get(className).getDeclaredMethod(methodName).getMethodInfo().getLineNumber(0);
                        if (line > 1) {
                            sbd.append("(").append(clazz.getSimpleName()).append(".java:").append(line - 1).append(")");
                        }
                    } catch (Exception ignore) {
                    }

                    sbd.append(", time: (");
                    long startTimeMillis = LogUtil.getStartTimeMillis();
                    if (U.greater0(startTimeMillis)) {
                        sbd.append(DateUtil.formatDateTimeMs(new Date(startTimeMillis))).append(" -> ");
                    }
                    long currentTimeMillis = System.currentTimeMillis();
                    sbd.append(DateUtil.formatDateTimeMs(new Date(currentTimeMillis)));
                    if (U.greater0(startTimeMillis) && currentTimeMillis >= startTimeMillis) {
                        sbd.append(DateUtil.toHuman(currentTimeMillis - startTimeMillis));
                    }
                    sbd.append(")");

                    sbd.append(", return: (").append(json).append(")");
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
