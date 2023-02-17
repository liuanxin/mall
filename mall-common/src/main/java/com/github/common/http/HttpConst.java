package com.github.common.http;

import com.github.common.util.A;
import com.github.common.util.U;

import java.util.LinkedHashMap;
import java.util.Map;

final class HttpConst {

    /** 建立连接的超时时间, 单位: 毫秒 */
    static final int CONNECT_TIME_OUT = 5000;
    /** 数据交互的时间, 单位: 毫秒 */
    static final int READ_TIME_OUT = 60000;

    /**
     * 连接池最大数量.
     *
     * okhttp 默认是 5. 见: {@link okhttp3.ConnectionPool}
     * apache httpClient 默认是 20. 见: {@link org.apache.http.impl.pool.BasicConnPool}
     */
    static final int POOL_MAX_TOTAL = 30;

    static String getUserAgent(String client) {
        return String.format("Mozilla/5.0 (%s; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36", client);
    }

    /** url 如果不是以 「http://」 或 「https://」 开头就加上 「http://」 */
    static String handleEmptyScheme(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        return url;
    }

    /** 处理 get 请求的参数: 拼在 url 上即可 */
    static String handleGetParams(String url, Map<String, Object> params) {
        if (A.isNotEmpty(params)) {
            url = U.appendUrl(url) + U.formatParam(false, params);
        }
        return url;
    }

    static Map<String, Object> handleContentType(Map<String, Object> headers, boolean requestBody) {
        Map<String, Object> headerMap = handleContentType(headers);
        headerMap.put("Content-Type", (requestBody ? "application/json" : "application/x-www-form-urlencoded"));
        return headerMap;
    }

    static Map<String, Object> handleContentType(Map<String, Object> headers) {
        Map<String, Object> headerMap = new LinkedHashMap<>();
        if (A.isNotEmpty(headers)) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                if (!"Content-Type".equalsIgnoreCase(entry.getKey())) {
                    headerMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return headerMap;
    }
}
