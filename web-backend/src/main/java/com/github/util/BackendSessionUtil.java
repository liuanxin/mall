package com.github.util;

import com.github.common.exception.NotLoginException;
import com.github.common.json.JsonUtil;
import com.github.common.mvc.AppTokenHandler;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.common.util.U;

import javax.servlet.http.HttpSession;

/** !!! 操作 session 都基于此, 其他地方不允许操作! 避免 session 被滥用 !!! */
public class BackendSessionUtil {

    /** 放在 session 里的图片验证码 key */
    private static final String CODE = BackendSessionUtil.class.getName() + "-CODE";
    /** 放在 session 里的用户 的 key */
    private static final String USER = BackendSessionUtil.class.getName() + "-USER";

    /** 将图片验证码的值放入 session */
    public static void putImageCode(String code) {
        HttpSession session = RequestUtil.getSession();
        if (U.isNotNull(session)) {
            session.setAttribute(CODE, code);
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("put image code({}) in session({})", code, session.getId());
            }
        }
    }
    /** 验证图片验证码 */
    public static boolean checkImageCode(String code) {
        if (U.isBlank(code)) {
            return false;
        }

        HttpSession session = RequestUtil.getSession();
        return U.isNotNull(session) && U.toStr(session.getAttribute(CODE)).equalsIgnoreCase(code);
    }

    /** 登录之后调用此方法, 将 用户信息 放入 session, app 需要将返回的数据保存到本地 */
    public static <T> String whenLogin(T user) {
        if (U.isNull(user)) {
            return U.EMPTY;
        }

        BackendSessionModel model = BackendSessionModel.assemblyData(user);
        if (U.isNull(model)) {
            return U.EMPTY;
        }

        HttpSession session = RequestUtil.getSession();
        if (U.isNotNull(session)) {
            String json = JsonUtil.toJson(model);
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("put ({}) in session({})", json, session.getId());
            }
            session.setAttribute(USER, json);
        }
        return AppTokenHandler.generateToken(model);
    }

    /** 获取用户信息. 没有则使用默认信息 */
    private static BackendSessionModel getSessionInfo() {
        // 1.token, 2.session, 3.默认值
        BackendSessionModel tokenModel = AppTokenHandler.getSessionInfoWithToken(BackendSessionModel.class);
        if (U.isNotNull(tokenModel)) {
            return tokenModel;
        }

        HttpSession session = RequestUtil.getSession();
        if (U.isNotNull(session)) {
            String json = U.toStr(session.getAttribute(USER));
            if (U.isNotBlank(json)) {
                BackendSessionModel model = JsonUtil.toObjectNil(json, BackendSessionModel.class);
                if (U.isNotNull(model)) {
                    return model;
                }
            }
        }

        return BackendSessionModel.defaultUser();
    }

    /** 从 session 中获取用户 id */
    public static Long getUserId() {
        return getSessionInfo().getId();
    }

    /** 从 session 中获取用户名 */
    public static String getUserName() {
        return getSessionInfo().getName();
    }

    public static String getUserInfo() {
        return getSessionInfo().userInfo();
    }

    /** 验证登录, 未登录则抛出异常 */
    public static void checkLogin() {
        if (!getSessionInfo().wasLogin()) {
            throw new NotLoginException();
        }
    }

    /** 退出登录时调用. 清空 session */
    public static void signOut() {
        HttpSession session = RequestUtil.getSession();
        if (U.isNotNull(session)) {
            session.invalidate();
        }
    }
}
