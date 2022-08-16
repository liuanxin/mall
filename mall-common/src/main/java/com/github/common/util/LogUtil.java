package com.github.common.util;

import com.github.common.date.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Date;
import java.util.Locale;

/** 日志工具 */
public final class LogUtil {

    /** 根日志: 在类里面使用 LoggerFactory.getLogger(XXX.class) 跟这种方式一样! */
    public static final Logger ROOT_LOG = LoggerFactory.getLogger("root");
    /** SQL 相关的日志 */
    public static final Logger SQL_LOG = LoggerFactory.getLogger("sqlLog");

    /** 接收到请求的时间, 在配置文件中使用 %X{RECORD_TIME} 获取  */
    private static final String RECEIVE_TIME_CONTEXT = "RECEIVE_TIME";
    /** 在日志上下文中记录的跟踪 id, 在配置文件中使用 %X{TRACE_ID} 获取 */
    private static final String TRACE_ID_CONTEXT = "TRACE_ID";

    /** 在日志上下文中记录的最早的时间, 不在配置中使用, 只在需要用到的地方基于日志上下文获取即可 */
    private static final String START_TIME = "START_TIME";
    /** 在日志上下文中记录的真实 ip, 不在配置中使用, 只在需要用到的地方基于日志上下文获取即可 */
    private static final String REAL_IP = "REAL_IP";
    /** 在日志上下文中记录的客户端语言, 不在配置中使用, 只在需要用到的地方基于日志上下文获取即可 */
    private static final String LANGUAGE = "LANGUAGE";

    /** 将 跟踪号 和 接收到请求的时间 放进日志上下文, 主要用在非 web 环境的地方(如定时任务, mq 消费等) */
    public static void putTraceId(String traceId) {
        if (U.lessAndEquals0(getStartTime())) {
            Date now = new Date();
            MDC.put(START_TIME, U.toStr(now.getTime()));
            MDC.put(RECEIVE_TIME_CONTEXT, DateUtil.formatDateTimeMs(now) + " -> ");
        }
        if (hasNotTraceId()) {
            // 跟踪号放在最后, 因此在最前加一个空格
            MDC.put(TRACE_ID_CONTEXT, " " + U.defaultIfBlank(traceId, U.uuid16()));
        }
    }
    /** 将 跟踪号 和 接收到请求的时间 和 ip 放进日志上下文, 主要用在有 web 环境的地方 */
    public static void putTraceAndIp(String traceId, String ip, Locale locale) {
        putTraceId(traceId);
        if (U.isNotBlank(ip)) {
            MDC.put(REAL_IP, ip);
        }
        if (U.isNotNull(locale)) {
            MDC.put(LANGUAGE, locale.toString().replace("_", "-"));
        }
    }

    public static boolean hasNotTraceId() {
        return U.isBlank(getTraceId());
    }

    public static long getStartTime() {
        return U.toLong(MDC.get(START_TIME));
    }
    public static String getTraceId() {
        return U.toStr(MDC.get(TRACE_ID_CONTEXT)).trim();
    }
    public static String getIp() {
        return U.toStr(MDC.get(REAL_IP)).trim();
    }
    public static String getLanguage() {
        return MDC.get(LANGUAGE);
    }

    public static void unbind() {
        MDC.clear();
    }
}
