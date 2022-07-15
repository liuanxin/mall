package com.github.common.util;

import com.github.common.date.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Date;

/** 日志管理, 使用此 utils 获取 log, 不要在类中使用 LoggerFactory.getLogger 的方式! */
public final class LogUtil {

    /** 根日志: 在类里面使用 LoggerFactory.getLogger(XXX.class) 跟这种方式一样! */
    public static final Logger ROOT_LOG = LoggerFactory.getLogger("root");
    /** SQL 相关的日志 */
    public static final Logger SQL_LOG = LoggerFactory.getLogger("sqlLog");

    /** 接收到请求的时间, 在配置文件中使用 %X{RECORD_TIME} 获取  */
    private static final String RECEIVE_TIME = "RECEIVE_TIME";
    /** 在日志上下文中记录的跟踪 id, 在配置文件中使用 %X{TRACE_ID} 获取 */
    private static final String TRACE_ID = "TRACE_ID";
    /** 在日志上下文中记录的基础信息: 包括 ip、url 等, 在配置文件中使用 %X{BASIC_INFO} 获取  */
    private static final String BASIC_INFO = "BASIC_INFO";
    /** 在日志上下文中记录的真实 ip, 在配置文件中使用 %X{REAL_IP} 获取 */
    private static final String REAL_IP = "REAL_IP";
    /** 在日志上下文中记录的用户信息, 在配置文件中使用 %X{USER} 获取 */
    private static final String USER = "USER";

    /** 将 跟踪号 和 接收到请求的时间 放进日志上下文 */
    public static void putTraceId(String traceId) {
        if (hasNotTraceId()) {
            MDC.put(RECEIVE_TIME, DateUtil.formatDateTimeMs(new Date()) + " -> ");
            // 跟踪号放在最后, 因此在最前加一个空格
            MDC.put(TRACE_ID, " " + U.defaultIfBlank(traceId, U.uuid16()));
        }
    }
    /** 将 跟踪号 和 接收到请求的时间 和 ip 放进日志上下文 */
    public static void putTraceAndIp(String traceId, String ip, String basicInfo) {
        putTraceId(traceId);
        if (U.isNotBlank(ip)) {
            MDC.put(REAL_IP, "(" + ip + ") ");
        }
        if (U.isNotBlank(basicInfo)) {
            MDC.put(BASIC_INFO, "[" + basicInfo + "] ");
        }
    }
    /** 将 跟踪号 和 接收到请求的时间 和 ip 放进日志上下文 */
    public static void putTraceAndIpAndUser(String traceId, String ip, String basicInfo, String userInfo) {
        putTraceAndIp(traceId, ip, basicInfo);
        if (U.isNotBlank(userInfo)) {
            MDC.put(USER, "(" + userInfo + ") ");
        }
    }

    public static boolean hasNotTraceId() {
        return U.isBlank(getTraceId());
    }

    public static String getTraceId() {
        return U.toStr(MDC.get(TRACE_ID)).trim();
    }
    public static String getIp() {
        return U.toStr(MDC.get(REAL_IP)).trim();
    }
    public static String getUser() {
        return U.toStr(MDC.get(USER)).trim();
    }

    public static void unbind() {
        MDC.clear();
    }
}
