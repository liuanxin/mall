package com.github.util;

import com.github.common.exception.ForbiddenException;
import com.github.common.exception.NotLoginException;
import com.github.common.json.JsonUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.common.util.U;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/** 操作 session 都基于此, 其他地方不允许操作! 避免 session 被滥用 */
@SuppressWarnings("ConstantConditions")
public class ManagerSessionUtil {

    /** 放在 session 里的图片验证码 key */
    private static final String CODE = ManagerSessionUtil.class.getName() + "-CODE";
    /** 放在 session 里的用户 的 key */
    private static final String USER = ManagerSessionUtil.class.getName() + "-USER";

    /** 验证图片验证码 */
    public static boolean checkImageCode(String code) {
        if (U.isBlank(code)) {
            return false;
        }

        Object securityCode = RequestUtil.getSession().getAttribute(CODE);
        return securityCode != null && code.equalsIgnoreCase(securityCode.toString());
    }
    /** 将图片验证码的值放入 session */
    public static void putImageCode(String code) {
        RequestUtil.getSession().setAttribute(CODE, code);
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("put image code ({}) in session ({})", code, RequestUtil.getSession().getId());
        }
    }

    /** 登录之后调用此方法, 将 用户信息、可访问的 url 等放入 session */
    public static <T,P> void whenLogin(T account, List<P> permissions) {
        ManagerSessionModel sessionModel = ManagerSessionModel.assemblyData(account, permissions);
        if (U.isNotNull(sessionModel)) {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("put ({}) in session({})",
                        JsonUtil.toJson(sessionModel), RequestUtil.getSession().getId());
            }
            RequestUtil.getSession().setAttribute(USER, sessionModel);
        }
    }

    /** 获取用户信息, 从 token 中获取, 没有则从 session 中获取 */
    private static ManagerSessionModel getSessionInfo() {
        ManagerSessionModel sessionModel = (ManagerSessionModel) RequestUtil.getSession().getAttribute(USER);
        return sessionModel == null ? ManagerSessionModel.defaultUser() : sessionModel;
    }

    /** 从 session 中获取用户 id */
    public static Long getUserId() {
        return getSessionInfo().getId();
    }

    /** 从 session 中获取用户名 */
    public static String getUserName() {
        return getSessionInfo().getUserName();
    }

    public static boolean hasAdmin() {
        return getSessionInfo().hasAdmin();
    }

    public static String getUserInfo() {
        return getSessionInfo().userInfo();
    }

    /** 验证登录, 未登录则抛出异常 */
    public static void checkLogin() {
        if (getSessionInfo().notLogin()) {
            throw new NotLoginException();
        }
    }

    /** 检查权限, 无权限则抛出异常 */
    public static void checkPermission() {
        ManagerSessionModel sessionModel = getSessionInfo();
        // 非超级管理员才验证权限
        if (sessionModel.notAdmin()) {
            HttpServletRequest request = RequestUtil.getRequest();
            String url = request.getRequestURI();
            String method = request.getMethod();

            if (sessionModel.notPermission(url, method)) {
                throw new ForbiddenException(String.format("您没有(%s)的 %s 访问权限", url, method));
            }
        }
    }

    /** 退出登录时调用. 清空 session */
    public static void signOut() {
        RequestUtil.getSession().invalidate();
    }
}
