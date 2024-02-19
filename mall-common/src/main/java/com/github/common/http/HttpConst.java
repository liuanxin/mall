package com.github.common.http;

import cn.hutool.core.util.ObjectUtil;
import com.github.common.Const;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;

import java.util.HashMap;
import java.util.Map;

final class HttpConst {

    /** 建立连接的超时时间, 单位: 毫秒 */
    static final int CONNECT_TIME_OUT = 5000;
    /** 数据交互的时间, 单位: 毫秒 */
    static final int READ_TIME_OUT = 60000;
    /**
     * <pre>
     * 连接池最大数量.
     *
     * okhttp 默认是 5. 见: {@link okhttp3.ConnectionPool}
     * apache httpClient 默认是 20. 见: {@link org.apache.http.impl.pool.BasicConnPool}
     * </pre>
     */
    static final int POOL_MAX_TOTAL = 100;

    /* static String getUserAgent(String client) {
        return String.format("Mozilla/5.0 (%s; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36", client);
    } */

    /** url 如果不是以 「http://」 或 「https://」 开头就加上 「http://」 */
    static String handleEmptyScheme(String url) {
        String checkUrl = url.trim().toLowerCase();
        if (!checkUrl.startsWith("http://") && !checkUrl.startsWith("https://")) {
            url = "http://" + url.trim();
        }
        return url;
    }

    /** 处理 get 请求的参数: 拼在 url 上即可 */
    static String appendParamsToUrl(String url, Map<String, Object> params) {
        if (A.isNotEmpty(params)) {
            return U.appendUrl(url) + U.formatParam(false, true, params);
        }
        return url;
    }

    /** 处理公共头 */
    static Map<String, Object> handleCommonHeader(Map<String, Object> headers /*, String userAgent*/ ) {
        Map<String, Object> returnMap = ObjectUtil.defaultIfNull(headers, new HashMap<>());
        /*
        if (U.isNotBlank(userAgent)) {
            returnMap.put("User-Agent", userAgent);
        }
        */
        String traceId = LogUtil.getTraceId();
        if (U.isNotBlank(traceId)) {
            returnMap.put(Const.TRACE, traceId);
        }
        String language = LogUtil.getLanguage();
        if (U.isNotBlank(language)) {
            returnMap.put("Accept-Language", language);
        }
        return returnMap;
    }
}
