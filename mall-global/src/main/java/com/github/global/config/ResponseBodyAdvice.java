package com.github.global.config;

import com.github.common.date.DateUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.common.util.U;
import com.github.global.constant.GlobalConst;
import javassist.ClassPool;
import lombok.RequiredArgsConstructor;
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

@SuppressWarnings("NullableProblems")
@RequiredArgsConstructor
@ConditionalOnClass({ HttpServletResponse.class, ResponseBody.class })
@ControllerAdvice(annotations = { Controller.class, RestController.class })
public class ResponseBodyAdvice extends AbstractMappingJacksonResponseBodyAdvice {

    private static final ClassPool CLASS_POOL = new ClassPool(ClassPool.getDefault());

    private final GlobalLogHandler logHandler;

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType, MethodParameter parameter,
                                           ServerHttpRequest request, ServerHttpResponse response) {
        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            if (GlobalConst.EXCLUDE_PATH_SET.contains(RequestUtil.getRequestUri())) {
                return;
            }

            StringBuilder sbd = new StringBuilder();
            long startTime = LogUtil.getStartTime();
            if (U.greater0(startTime)) {
                sbd.append("time(").append(DateUtil.toHuman(System.currentTimeMillis() - startTime)).append(") ");
            }

            Class<?> clazz = parameter.getContainingClass();
            String className = clazz.getName();
            sbd.append(className);

            Method method = parameter.getMethod();
            String methodName = U.isNotNull(method) ? method.getName() : U.EMPTY;
            if (U.isNotBlank(methodName)) {
                sbd.append("#").append(methodName);
                try {
                    String classInFile = U.getClassInFile(clazz);
                    if (U.isNotBlank(classInFile)) {
                        CLASS_POOL.appendClassPath(classInFile);
                        int line = CLASS_POOL.get(className).getDeclaredMethod(methodName).getMethodInfo().getLineNumber(0);
                        if (line > 1) {
                            sbd.append("(").append(clazz.getSimpleName()).append(".java:").append(line - 1).append(")");
                        }
                        sbd.append(" in (").append(classInFile).append(")");
                    }
                } catch (Exception e) {
                    if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                        LogUtil.ROOT_LOG.debug("get {}#{} line-number exception", className, methodName, e);
                    }
                }
            }

            sbd.append(" return(").append(logHandler.toJson(bodyContainer.getValue())).append(")");
            LogUtil.ROOT_LOG.info(sbd.toString());
        }
    }
}
