package com.github.common.http;

import com.github.common.date.DateUtil;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.google.common.base.Joiner;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("DuplicatedCode")
public class HttpClientUtil {

    private static final String USER_AGENT = HttpConst.getUserAgent("http_client");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofMillis(HttpConst.CONNECT_TIME_OUT))
            .build();


    /** 向指定 url 进行 get 请求 */
    public static String get(String url) {
        return get(url, null);
    }
    /** 向指定 url 进行 get 请求. 有参数 */
    public static String get(String url, Map<String, Object> params) {
        return getWithHeader(url, params, null);
    }
    /** 向指定 url 进行 get 请求. 有参数和头 */
    public static String getWithHeader(String url, Map<String, Object> params, Map<String, Object> headerMap) {
        return handleRequest("GET", U.appendUrl(url) + U.formatParam(false, params), null, headerMap);
    }


    /** 向指定的 url 进行 post 请求. 有参数 */
    public static String post(String url, Map<String, Object> params) {
        return postWithHeader(url, params, null);
    }
    /** 向指定的 url 进行 post 请求. 有参数和头 */
    public static String postWithHeader(String url, Map<String, Object> params, Map<String, Object> headers) {
        return handleRequest("POST", url, U.formatParam(false, params), headers);
    }


    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static String postBody(String url, Map<String, Object> params) {
        return postBodyWithHeader(url, params, null);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static String postBodyWithHeader(String url, Map<String, Object> params, Map<String, Object> headers) {
        return postBodyWithHeader(url, JsonUtil.toJson(params), headers);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static String postBody(String url, String json) {
        return postBodyWithHeader(url, json, null);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static String postBodyWithHeader(String url, String json, Map<String, Object> headers) {
        Map<String, Object> headerMap = new LinkedHashMap<>();
        if (A.isNotEmpty(headers)) {
            headerMap.putAll(headers);
        }
        headerMap.put("Content-Type", "application/json");
        return handleRequest("POST", url, U.toStr(json), headerMap);
    }


//    /** 向指定 url 上传文件 */
//    public static String postFile(String url, Map<String, Object> params, Map<String, Object> headers, File file) {
//    }

    private static String handleRequest(String method, String url, String data, Map<String, Object> headers) {
        long start = System.currentTimeMillis();
        Map<String, List<String>> reqHeaders = null;
        String resCode = "";
        Map<String, List<String>> resHeaders = null;
        String result = null;

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder();
            builder.method(method, U.isBlank(data) ?  HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(data));
            builder.uri(URI.create(url));
            if (A.isNotEmpty(headers)) {
                for (Map.Entry<String, Object> entry : headers.entrySet()) {
                    builder.setHeader(entry.getKey(), U.toStr(entry.getValue()));
                }
            }
            builder.setHeader("User-Agent", USER_AGENT);
            HttpRequest request = builder.build();
            reqHeaders = request.headers().map();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            resCode = response.statusCode() + " ";
            resHeaders = response.headers().map();
            result = response.body();
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(collectContext(start, method, url, data, reqHeaders, resCode, resHeaders, result));
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(collectContext(start, method, url, data, reqHeaders, resCode, resHeaders, result), e);
            }
        }
        return result;
    }
    /** 收集上下文中的数据, 以便记录日志 */
    private static String collectContext(long start, String method, String url, String params,
                                         Map<String, List<String>> reqHeaders, String resCode,
                                         Map<String, List<String>> resHeaders, String result) {
        StringBuilder sbd = new StringBuilder();
        long now = System.currentTimeMillis();
        sbd.append("HttpClient => [")
                .append(DateUtil.formatDateTimeMs(new Date(start))).append(" -> ")
                .append(DateUtil.formatDateTimeMs(new Date(now)))
                .append("(").append(DateUtil.toHuman(now - start)).append(")")
                .append("] (").append(method).append(" ").append(url).append(")");
        sbd.append(" req[");
        boolean hasParam = U.isNotBlank(params);
        if (hasParam) {
            sbd.append("param(").append(U.compress(params)).append(")");
        }
        boolean hasReqHeader = A.isNotEmpty(reqHeaders);
        if (hasParam && hasReqHeader) {
            sbd.append(" ");
        }
        if (hasReqHeader) {
            sbd.append("header(");
            for (Map.Entry<String, List<String>> entry : reqHeaders.entrySet()) {
                sbd.append("<").append(entry.getKey()).append(": ").append(Joiner.on(",").join(entry.getValue())).append(">");
            }
            sbd.append(")");
        }
        sbd.append("], res[").append(resCode);
        boolean hasResHeader = A.isNotEmpty(resHeaders);
        if (hasResHeader) {
            sbd.append("header(");
            for (Map.Entry<String, List<String>> entry : resHeaders.entrySet()) {
                sbd.append("<").append(entry.getKey()).append(": ").append(Joiner.on(",").join(entry.getValue())).append(">");
            }
            sbd.append(")");
        }
        boolean hasResult = U.isNotBlank(result);
        if (hasResHeader && hasResult) {
            sbd.append(" ");
        }
        if (hasResult) {
            sbd.append("return(").append(U.compress(result)).append(")");
        }
        sbd.append("]");
        return sbd.toString();
    }
}
