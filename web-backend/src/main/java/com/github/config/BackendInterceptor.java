package com.github.config;

import com.github.common.annotation.NeedLogin;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.util.BackendSessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Configuration
@SuppressWarnings("NullableProblems")
public class BackendInterceptor implements HandlerInterceptor {

    /** 打印请求日志时, 是否输出头信息 */
    @Value("${req.logPrintHeader:true}")
    private boolean printHeader;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        printParam();
        checkLoginAndPermission(handler);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest req, HttpServletResponse res, Object handler, ModelAndView mv) {
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception e) {
        if (e != null) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("request was over, but have exception", e);
            }
        }
    }

    private void printParam() {
        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            String userInfo = BackendSessionUtil.getUserInfo();
            String basicInfo = RequestUtil.logBasicInfo();
            String requestInfo = RequestUtil.logRequestInfo(printHeader);
            LogUtil.ROOT_LOG.info("[{}] [{}] [{}]", userInfo, basicInfo, requestInfo);
        }
    }

    /** 检查登录 */
    private void checkLoginAndPermission(Object handler) {
        if (!handler.getClass().isAssignableFrom(HandlerMethod.class)) {
            return;
        }

        HandlerMethod method = (HandlerMethod) handler;
        NeedLogin needLogin = method.getMethodAnnotation(NeedLogin.class);
        if (needLogin == null) {
            needLogin = AnnotationUtils.findAnnotation(method.getBeanType(), NeedLogin.class);
        }
        // 标注了 @NeedLogin 且 flag 为 true(默认就是 true)则表示当前请求需要登录
        if (needLogin != null && needLogin.value()) {
            BackendSessionUtil.checkLogin();
        }
    }
}
