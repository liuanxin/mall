package com.github.common.http;

import com.github.common.Const;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import okhttp3.*;

import java.io.File;
import java.net.URLConnection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DuplicatedCode")
public class OkHttpClientUtil {

    // MIME 说明: http://www.w3school.com.cn/media/media_mimeref.asp

    // private static final String USER_AGENT = HttpConst.getUserAgent("okhttp3");

    /**
     * 连接保持时间, 单位: 分, 默认是 5. 见: {@link okhttp3.ConnectionPool}
     */
    private static final int CONNECTION_KEEP_ALIVE_TIME = 5;

    // 请求有三种方式:
    // 1. 普通表单: <form ...> 的方式(Content-Type: application/x-www-form-urlencoded)
    // 2. 文件上传表单: <from type="multipart/form-data" ...> 的方式(Content-Type: multipart/form-data)
    // 3. 请求体: POST|PUT|DELETE + RequestBody 的方式(Content-Type: application/json)
    private static final MediaType FORM = MediaType.get("application/x-www-form-urlencoded");
    private static final MediaType FORM_DATA = MediaType.get("multipart/form-data");
    private static final MediaType JSON = MediaType.get("application/json");

    private static final OkHttpClient HTTP_CLIENT;
    static {
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder()
                .connectTimeout(HttpConst.CONNECT_TIME_OUT, TimeUnit.MILLISECONDS)
                .readTimeout(HttpConst.READ_TIME_OUT, TimeUnit.MILLISECONDS)
                // .followRedirects(true) // 默认就会处理重定向
                // .followSslRedirects(true) // 默认就会处理 ssl 的重定向
                .connectionPool(new ConnectionPool(HttpConst.POOL_MAX_TOTAL, CONNECTION_KEEP_ALIVE_TIME, TimeUnit.MINUTES))
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    TimeoutConfig tag = request.tag(TimeoutConfig.class);
                    if (U.isNotNull(tag)) {
                        chain.withReadTimeout(tag.readTimeout, TimeUnit.MILLISECONDS);
                    }
                    return chain.proceed(request);
                });
        if (TrustCerts.IGNORE_SSL) {
            builder.sslSocketFactory(TrustCerts.IGNORE_SSL_FACTORY, TrustCerts.TRUST_MANAGER);
        }
        HTTP_CLIENT = builder.build();
    }

    record TimeoutConfig(int readTimeout) {}


    /** 向指定 url 进行 get 请求(普通表单方式) */
    public static HttpData get(String url) {
        return get(url, null);
    }
    /** 向指定 url 进行 get 请求(普通表单方式) */
    public static HttpData get(String url, Map<String, Object> params) {
        return get(url, params, null);
    }
    /** 向指定 url 进行 get 请求(普通表单方式) */
    public static HttpData get(String url, Map<String, Object> params, Map<String, Object> headers) {
        return get(url, params, headers, true);
    }
    /** 向指定 url 进行 get 请求(普通表单方式) */
    public static HttpData get(String url, Map<String, Object> params, Map<String, Object> headers, boolean printLog) {
        return get(url, 0, params, headers, printLog);
    }
    /** 向指定 url 进行 get 请求(普通表单方式) */
    public static HttpData get(String url, int timeoutSecond, Map<String, Object> params,
                               Map<String, Object> headers, boolean printLog) {
        String useUrl = HttpConst.appendParamsToUrl(url, params);
        Request.Builder builder = new Request.Builder();
        handleHeader(builder, headers);
        return handleRequest(useUrl, timeoutSecond, builder, null, null, printLog);
    }

    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static HttpData postUrlEncode(String url, Map<String, Object> params) {
        return postUrlEncode(url, params, null);
    }
    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static HttpData postUrlEncode(String url, Map<String, Object> params,
                                         Map<String, Object> headers) {
        return postUrlEncode(url, params, headers, true);
    }
    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static HttpData postUrlEncode(String url, Map<String, Object> params,
                                         Map<String, Object> headers, boolean printLog) {
        return postUrlEncode(url, 0, params, headers, printLog);
    }
    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static HttpData postUrlEncode(String url, int timeoutSecond, Map<String, Object> params,
                                         Map<String, Object> headers, boolean printLog) {
        RequestBody requestBody = RequestBody.create(U.formatParam(false, true, params), FORM);
        Request.Builder builder = new Request.Builder().post(requestBody);
        handleHeader(builder, headers);
        return handleRequest(url, timeoutSecond, builder, params, null, printLog);
    }

    /** 向指定的 url 基于 post 发起请求 */
    public static HttpData post(String url, String json) {
        return post(url, null, json, null);
    }
    /** 向指定的 url 基于 post 发起请求 */
    public static HttpData post(String url, Map<String, Object> params, String json) {
        return post(url, params, json, null);
    }
    /** 向指定的 url 基于 post 发起请求 */
    public static HttpData post(String url, String json, Map<String, Object> headers) {
        return post(url, null, json, headers);
    }
    /** 向指定的 url 基于 post 发起请求 */
    public static HttpData post(String url, Map<String, Object> params, String json,
                                Map<String, Object> headers) {
        return post(url, params, json, headers, true);
    }
    /** 向指定的 url 基于 post 发起请求 */
    public static HttpData post(String url, Map<String, Object> params, String json,
                                Map<String, Object> headers, boolean printLog) {
        return post(url, 0, params, json, headers, printLog);
    }
    /** 向指定的 url 基于 post 发起请求 */
    public static HttpData post(String url, int timeoutSecond, Map<String, Object> params,
                                String json, Map<String, Object> headers, boolean printLog) {
        String content = U.toStr(json);
        String useUrl = HttpConst.appendParamsToUrl(url, params);
        Request.Builder builder = new Request.Builder().post(RequestBody.create(content, JSON));
        handleHeader(builder, headers);
        return handleRequest(useUrl, timeoutSecond, builder, null, content, printLog);
    }


    /** 向指定的 url 基于 put 发起请求 */
    public static HttpData put(String url, String json) {
        return put(url, null, json, null);
    }
    /** 向指定的 url 基于 put 发起请求 */
    public static HttpData put(String url, Map<String, Object> params, String json) {
        return put(url, params, json, null);
    }
    /** 向指定的 url 基于 put 发起请求 */
    public static HttpData put(String url, String json, Map<String, Object> headers) {
        return put(url, null, json, headers);
    }
    /** 向指定的 url 基于 put 发起请求 */
    public static HttpData put(String url, Map<String, Object> params, String json, Map<String, Object> headers) {
        return put(url, params, json, headers, true);
    }
    /** 向指定的 url 基于 put 发起请求 */
    public static HttpData put(String url, Map<String, Object> params, String json,
                               Map<String, Object> headers, boolean printLog) {
        return put(url, 0, params, json, headers, printLog);
    }
    /** 向指定的 url 基于 put 发起请求 */
    public static HttpData put(String url, int timeoutSecond, Map<String, Object> params,
                               String json, Map<String, Object> headers, boolean printLog) {
        String content = U.toStr(json);
        String useUrl = HttpConst.appendParamsToUrl(url, params);
        RequestBody requestBody = RequestBody.create(content, JSON);
        Request.Builder builder = new Request.Builder().put(requestBody);
        handleHeader(builder, headers);
        return handleRequest(useUrl, timeoutSecond, builder, null, content, printLog);
    }

    /** 向指定的 url 基于 delete 发起请求 */
    public static HttpData delete(String url, String data) {
        return delete(url, data, null);
    }
    /** 向指定的 url 基于 delete 发起请求 */
    public static HttpData delete(String url, String json, Map<String, Object> headers) {
        return delete(url, json, headers, true);
    }
    /** 向指定的 url 基于 delete 发起请求 */
    public static HttpData delete(String url, String json, Map<String, Object> headers, boolean printLog) {
        return delete(url, 0, json, headers, printLog);
    }
    /** 向指定的 url 基于 delete 发起请求 */
    public static HttpData delete(String url, int timeoutSecond, String json,
                                  Map<String, Object> headers, boolean printLog) {
        String content = U.toStr(json);
        RequestBody requestBody = RequestBody.create(content, JSON);
        Request.Builder builder = new Request.Builder().delete(requestBody);
        handleHeader(builder, headers);
        return handleRequest(url, timeoutSecond, builder, null, content, printLog);
    }


    /** 向指定 url 上传文件(基于 POST + form-data 的方式) */
    public static HttpData uploadFile(String url, Map<String, File> files) {
        return uploadFile(url, null, null, null, files);
    }
    /** 向指定 url 上传文件(基于 POST + form-data 的方式) */
    public static HttpData uploadFile(String url, Map<String, Object> params, Map<String, File> files) {
        return uploadFile(url, null, null, params, files);
    }
    /** 向指定 url 上传文件(基于 POST + form-data 的方式) */
    public static HttpData uploadFile(String url, Map<String, Object> headers,
                                      Map<String, Object> params, Map<String, File> files) {
        return uploadFile(url, null, headers, params, files);
    }
    /** 向指定 url 上传文件(基于 POST + form-data 的方式) */
    public static HttpData uploadFile(String url, String method, Map<String, Object> headers,
                                      Map<String, Object> params, Map<String, File> files) {
        return uploadFile(url, method, 0, headers, params, files, true);
    }
    /** 向指定 url 上传文件(基于 POST + form-data 的方式) */
    public static HttpData uploadFile(String url, String method, int timeoutSecond, Map<String, Object> headers,
                                      Map<String, Object> params, Map<String, File> files, boolean printLog) {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(FORM_DATA);
        if (A.isNotEmpty(params)) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = U.toStr(entry.getValue());

                builder.addFormDataPart(key, value);
            }
        }
        Map<String, String> fileMap = new LinkedHashMap<>();
        if (A.isNotEmpty(files)) {
            for (Map.Entry<String, File> entry : files.entrySet()) {
                File file = entry.getValue();
                if (U.isNotNull(file)) {
                    String key = entry.getKey();
                    String fileName = file.getName();
                    MediaType type = MediaType.parse(URLConnection.guessContentTypeFromName(file.getName()));
                    builder.addFormDataPart(key, fileName, RequestBody.create(file, type));
                    fileMap.put(key, file.toString());
                }
            }
        }

        Request.Builder request;
        if ("PUT".equalsIgnoreCase(method)) {
            request = new Request.Builder().put(builder.build());
        } else {
            request = new Request.Builder().post(builder.build());
        }
        handleHeader(request, headers);
        return handleRequest(url, timeoutSecond, request, params, JsonUtil.toJsonNil(fileMap), printLog);
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
    private static HttpData handleRequest(String url, int timeoutSecond, Request.Builder builder,
                                          Map<String, Object> params, String body, boolean printLog) {
        if (timeoutSecond > 0) {
            builder.tag(TimeoutConfig.class, new TimeoutConfig(timeoutSecond));
        }
        String traceId = LogUtil.getTraceId();
        if (U.isNotBlank(traceId)) {
            builder.header(Const.TRACE, traceId);
        }
        String language = LogUtil.getLanguage();
        if (U.isNotBlank(language)) {
            builder.header("Accept-Language", language);
        }
        url = HttpConst.handleEmptyScheme(url);
        // Request request = builder.header("User-Agent", USER_AGENT).url(url).build();
        Request request = builder.url(url).build();
        String method = request.method();

        HttpData httpData = new HttpData();
        httpData.fillReq(method, url, handleHeader(request.headers()), U.formatPrintParam(params), body);
        try (
                Response response = HTTP_CLIENT.newCall(request).execute();
                ResponseBody responseBody = response.body()
        ) {
            int statusCode = response.code();
            Map<String, Object> resHeader = handleHeader(response.headers());
            String result = U.isNotNull(responseBody) ? responseBody.string() : null;
            httpData.fillRes(statusCode, resHeader, result);
            if (printLog && LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(httpData.toString());
            }
        } catch (Exception e) {
            httpData.fillException(e);
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(httpData.toString(), e);
            }
        }
        return httpData;
    }
    private static Map<String, Object> handleHeader(Headers resHeaders) {
        if (U.isNotNull(resHeaders)) {
            Map<String, Object> returnMap = new LinkedHashMap<>();
            for (String key : resHeaders.names()) {
                returnMap.put(key, resHeaders.get(key));
            }
            return returnMap;
        }
        return Collections.emptyMap();
    }
}
