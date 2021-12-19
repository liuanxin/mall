package com.github.config;

import com.github.common.Const;
import com.github.common.annotation.NotNeedLogin;
import com.github.common.annotation.NotNeedPermission;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.util.ManagerSessionUtil;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("NullableProblems")
public class ManagerInterceptor implements HandlerInterceptor {

    private static final List<String> LET_IT_GO = Arrays.asList(
            "/error", "/api/project", "/api/info", "/api/example/*"
    );

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        bindParam();
        checkLoginAndPermission(req.getRequestURI(), handler);
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
        unbindParam();
    }

    private void bindParam() {
        String traceId = RequestUtil.getCookieOrHeaderOrParam(Const.TRACE);
        LogUtil.putContext(traceId, RequestUtil.logContextInfo());
        LogUtil.putIp(RequestUtil.getRealIp());
        LogUtil.putUser(ManagerSessionUtil.getUserInfo());
    }

    private void unbindParam() {
        LogUtil.unbind();
    }

    /** 检查登录及权限 */
    private void checkLoginAndPermission(String uri, Object handler) {
        for (String letItGo : LET_IT_GO) {
            if (letItGo.equals(uri)) {
                return;
            }
            if (letItGo.contains("*")) {
                letItGo = letItGo.replace("*", "(.*)?");
                if (uri.matches(letItGo)) {
                    return;
                }
            }
        }
        if (!handler.getClass().isAssignableFrom(HandlerMethod.class)) {
            return;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 在不需要登录的 url 上标注 @NotNeedLogin
        NotNeedLogin notNeedLogin = getAnnotation(handlerMethod, NotNeedLogin.class);
        // 标注了 NotNeedLogin 且 flag 为 true(默认就是 true)则表示当前的请求不需要验证登录
        if (notNeedLogin != null && notNeedLogin.value()) {
            return;
        }
        // 检查登录
        ManagerSessionUtil.checkLogin();

        // 在不需要验证权限的 url 上标注 @NotNeedPermission
        NotNeedPermission notNeedPermission = getAnnotation(handlerMethod, NotNeedPermission.class);
        // 标注了 NotNeedPermission 且 flag 为 true(默认就是 true)则表示当前的请求不需要验证权限
        if (notNeedPermission != null && notNeedPermission.value()) {
            return;
        }
        // 检查权限
        ManagerSessionUtil.checkPermission();
    }
    private <T extends Annotation> T getAnnotation(HandlerMethod handlerMethod, Class<T> clazz) {
        // 先找方法上的注解, 没有再找类上的注解
        T annotation = handlerMethod.getMethodAnnotation(clazz);
        return annotation == null ? AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), clazz) : annotation;
    }
}
