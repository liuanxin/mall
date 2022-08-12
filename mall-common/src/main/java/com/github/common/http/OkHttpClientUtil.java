package com.github.common.http;

import com.github.common.Const;
import com.github.common.date.DateUtil;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DuplicatedCode")
public class OkHttpClientUtil {

    // MIME 说明: http://www.w3school.com.cn/media/media_mimeref.asp

    private static final String USER_AGENT = HttpConst.getUserAgent("okhttp3");

    /** 连接池最大数量 */
    private static final int MAX_CONNECTIONS = 200;
    /** 连接保持时间, 单位: 分 */
    private static final int CONNECTION_KEEP_ALIVE_TIME = 5;

    private static final MediaType JSON = MediaType.parse("application/json");

    private static final OkHttpClient HTTP_CLIENT;
    static {
        HTTP_CLIENT = new OkHttpClient().newBuilder()
                .connectTimeout(HttpConst.CONNECT_TIME_OUT, TimeUnit.MILLISECONDS)
                .readTimeout(HttpConst.READ_TIME_OUT, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(MAX_CONNECTIONS, CONNECTION_KEEP_ALIVE_TIME, TimeUnit.MINUTES))
                .build();
    }

    /** 向指定 url 进行 get 请求 */
    public static String get(String url) {
        return handleRequest(url, new Request.Builder(), null);
    }
    /** 向指定 url 进行 get 请求. 有参数 */
    public static String get(String url, Map<String, Object> params) {
        url = handleGetParams(url, params);
        return handleRequest(url, new Request.Builder(), U.formatParam(params));
    }
    /** 向指定 url 进行 get 请求. 有参数和头 */
    public static String getWithHeader(String url, Map<String, Object> params, Map<String, Object> headerMap) {
        url = handleGetParams(url, params);
        Request.Builder builder = new Request.Builder();
        handleHeader(builder, headerMap);
        return handleRequest(url, builder, U.formatParam(params));
    }


    /** 向指定的 url 进行 post 请求. 有参数 */
    public static String post(String url, Map<String, Object> params) {
        return postWithHeader(url, params, null);
    }
    /** 向指定的 url 进行 post 请求. 有参数和头 */
    public static String postWithHeader(String url, Map<String, Object> params, Map<String, Object> headers) {
        RequestBody request = RequestBody.create(MultipartBody.FORM, U.formatParam(false, params));
        Request.Builder builder = new Request.Builder().post(request);
        handleHeader(builder, headers);
        return handleRequest(url, builder, U.formatParam(params));
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
        String data = U.toStr(json);
        Request.Builder builder = new Request.Builder().post(RequestBody.create(JSON, json));
        handleHeader(builder, headers);
        builder.addHeader("Content-Type", "application/json");
        return handleRequest(url, builder, json);
    }


    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static String put(String url, String json) {
        return postBodyWithHeader(url, json, null);
    }
    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static String putWithHeader(String url, String json, Map<String, Object> headers) {
        String data = U.toStr(json);
        Request.Builder builder = new Request.Builder().put(RequestBody.create(JSON, json));
        handleHeader(builder, headers);
        builder.addHeader("Content-Type", "application/json");
        return handleRequest(url, builder, json);
    }


    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static String delete(String url, String json) {
        return postBodyWithHeader(url, json, null);
    }
    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static String delete(String url, String json, Map<String, Object> headers) {
        String data = U.toStr(json);
        Request.Builder builder = new Request.Builder().delete(RequestBody.create(JSON, json));
        handleHeader(builder, headers);
        builder.addHeader("Content-Type", "application/json");
        return handleRequest(url, builder, json);
    }


    /** 向指定 url 上传文件 */
    public static String postFile(String url, Map<String, Object> params, Map<String, Object> headers, Map<String, File> files) {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (U.isNotNull(value)) {
                builder.addFormDataPart(entry.getKey(), value.toString());
            }
        }
        for (Map.Entry<String, File> entry : files.entrySet()) {
            File file = entry.getValue();
            if (U.isNotNull(file)) {
                try {
                    MediaType type = MediaType.parse(Files.probeContentType(file.toPath()));
                    builder.addFormDataPart(entry.getKey(), null, RequestBody.create(type, file));
                } catch (IOException e) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error("add file({}) to post exception", file.getName(), e);
                    }
                }
            }
        }
        Request.Builder request = new Request.Builder().post(builder.build());
        handleHeader(request, headers);
        return handleRequest(url, request, U.formatParam(params));
    }


    /** url 如果不是以 「http://」 或 「https://」 开头就加上 「http://」 */
    private static String handleEmptyScheme(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        return url;
    }
    /** 处理 get 请求的参数: 拼在 url 上即可 */
    private static String handleGetParams(String url, Map<String, Object> params) {
        if (A.isNotEmpty(params)) {
            url = U.appendUrl(url) + U.formatParam(false, params);
        }
        return url;
    }
    /** 处理请求时存到 header 中的数据 */
    private static void handleHeader(Request.Builder request, Map<String, Object> headers) {
        if (A.isNotEmpty(headers)) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (U.isNotNull(value)) {
                    request.addHeader(key, value.toString());
                }
            }
        }
    }
    /** 发起 http 请求 */
    private static String handleRequest(String url, Request.Builder builder, String params) {
        url = handleEmptyScheme(url);

        String traceId = LogUtil.getTraceId();
        if (U.isNotBlank(traceId)) {
            builder.header(Const.TRACE, traceId);
        }
        Request request = builder.header("User-Agent", USER_AGENT).url(url).build();
        String method = request.method();
        Headers reqHeaders = request.headers();
        Headers resHeaders = null;
        String statusCode = "";
        String result = null;
        long start = System.currentTimeMillis();
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            resHeaders = response.headers();
            statusCode = response.code() + " ";
            // noinspection ConstantConditions
            result = response.body().string();
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(collectContext(start, method, url, params, reqHeaders, statusCode, resHeaders, result));
            }
            return result;
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(collectContext(start, method, url, params, reqHeaders, statusCode, resHeaders, result), e);
            }
        }
        return null;
    }
    /** 收集上下文中的数据, 以便记录日志 */
    private static String collectContext(long start, String method, String url, String params, Headers reqHeaders,
                                         String statusCode, Headers resHeaders, String result) {
        StringBuilder sbd = new StringBuilder();
        long now = System.currentTimeMillis();
        sbd.append("OkHttp3 => [")
                .append(DateUtil.formatDateTimeMs(new Date(start))).append(" -> ")
                .append(DateUtil.formatDateTimeMs(new Date(now)))
                .append("(").append(DateUtil.toHuman(now - start)).append(")")
                .append("] (").append(method).append(" ").append(url).append(")");
        sbd.append(" req[");
        boolean hasParam = U.isNotBlank(params);
        if (hasParam) {
            sbd.append("param(").append(U.compress(params)).append(")");
        }
        boolean hasReqHeader = U.isNotNull(reqHeaders);
        if (hasParam && hasReqHeader) {
            sbd.append(" ");
        }
        if (hasReqHeader) {
            sbd.append("header(");
            for (String name : reqHeaders.names()) {
                sbd.append("<").append(name).append(": ").append(reqHeaders.get(name)).append(">");
            }
            sbd.append(")");
        }
        sbd.append("], res[").append(statusCode);
        boolean hasResHeader = U.isNotNull(resHeaders);
        if (hasResHeader) {
            sbd.append(" header(");
            for (String name : resHeaders.names()) {
                sbd.append("<").append(name).append(":").append(resHeaders.get(name)).append(">");
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
