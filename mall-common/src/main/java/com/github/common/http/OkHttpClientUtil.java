package com.github.common.http;

import com.github.common.Const;
import com.github.common.date.DateUtil;
import com.github.common.util.A;
import com.github.common.util.DesensitizationUtil;
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

    /** 连接保持时间, 单位: 分, 默认是 5. 见: {@link okhttp3.ConnectionPool} */
    private static final int CONNECTION_KEEP_ALIVE_TIME = 5;

    // 请求有三种方式:
    // 1. 普通表单: <form ...> 的方式(Content-Type: application/x-www-form-urlencoded)
    // 2. 文件上传表单: <from type="multipart/form-data" ...> 的方式(Content-Type: multipart/form-data)
    // 3. 请求体: POST|PUT|DELETE + RequestBody 的方式(Content-Type: application/json)
    private static final MediaType FORM = MediaType.get("application/x-www-form-urlencoded");
    private static final MediaType FORM_DATA = MediaType.get("multipart/form-data");
    private static final MediaType JSON = MediaType.get("application/json");

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder()
            .connectTimeout(HttpConst.CONNECT_TIME_OUT, TimeUnit.MILLISECONDS)
            .readTimeout(HttpConst.READ_TIME_OUT, TimeUnit.MILLISECONDS)
            .connectionPool(new ConnectionPool(HttpConst.POOL_MAX_TOTAL, CONNECTION_KEEP_ALIVE_TIME, TimeUnit.MINUTES))
            .build();


    /** 向指定 url 进行 get 请求(普通表单方式) */
    public static ResponseData get(String url) {
        return get(url, null);
    }
    /** 向指定 url 进行 get 请求(普通表单方式) */
    public static ResponseData get(String url, Map<String, Object> params) {
        return get(url, params, null);
    }
    /** 向指定 url 进行 get 请求(普通表单方式) */
    public static ResponseData get(String url, Map<String, Object> params, Map<String, Object> headers) {
        Request.Builder builder = new Request.Builder();
        handleHeader(builder, HttpConst.handleContentType(headers, false));
        return handleRequest(HttpConst.appendParamsToUrl(url, params), builder, null);
    }


    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static ResponseData postWithForm(String url, Map<String, Object> params) {
        return postWithForm(url, params, null);
    }
    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static ResponseData postWithForm(String url, Map<String, Object> params, Map<String, Object> headers) {
        RequestBody requestBody = RequestBody.create(U.formatParam(false, params), FORM);
        Request.Builder builder = new Request.Builder().post(requestBody);
        handleHeader(builder, HttpConst.handleContentType(headers, false));
        return handleRequest(url, builder, U.formatParam(params));
    }

    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithBody(String url, String data) {
        return postWithBody(url, data, null);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithBody(String url, String data, Map<String, Object> headers) {
        String content = U.toStr(data);
        Request.Builder builder = new Request.Builder().post(RequestBody.create(content, JSON));
        handleHeader(builder, HttpConst.handleContentType(headers, true));
        return handleRequest(url, builder, content);
    }


    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static ResponseData put(String url, String data) {
        return put(url, data, null);
    }
    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static ResponseData put(String url, String data, Map<String, Object> headers) {
        String content = U.toStr(data);
        RequestBody requestBody = RequestBody.create(content, JSON);
        Request.Builder builder = new Request.Builder().put(requestBody);
        handleHeader(builder, HttpConst.handleContentType(headers, true));
        return handleRequest(url, builder, content);
    }


    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static ResponseData delete(String url, String data) {
        return deleteWithHeader(url, data, null);
    }
    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static ResponseData deleteWithHeader(String url, String data, Map<String, Object> headers) {
        String content = U.toStr(data);
        RequestBody requestBody = RequestBody.create(content, JSON);
        Request.Builder builder = new Request.Builder().delete(requestBody);
        handleHeader(builder, HttpConst.handleContentType(headers, true));
        return handleRequest(url, builder, data);
    }


    /** 向指定 url 上传文件(基于 POST + form-data 的方式) */
    public static ResponseData uploadFile(String url, Map<String, Object> headers, Map<String, Object> params, Map<String, File> files) {
        return uploadFile(url, null, headers, params, files);
    }
    /** 向指定 url 上传文件(基于 POST + form-data 的方式) */
    public static ResponseData uploadFile(String url, String method, Map<String, Object> headers,
                                          Map<String, Object> params, Map<String, File> files) {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(FORM_DATA);
        StringBuilder sbd = new StringBuilder();
        boolean hasParam = A.isNotEmpty(params);
        if (hasParam) {
            sbd.append("param(");
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = U.toStr(entry.getValue());

                builder.addFormDataPart(key, value);
                sbd.append("<").append(key).append(" : ").append(DesensitizationUtil.desByKey(key, value)).append(">");
            }
            sbd.append(")");
        }
        boolean hasFile = A.isNotEmpty(files);
        if (hasParam && hasFile) {
            sbd.append(" ");
        }
        if (hasFile) {
            sbd.append("file(");
            for (Map.Entry<String, File> entry : files.entrySet()) {
                File file = entry.getValue();
                if (U.isNotNull(file)) {
                    String key = entry.getKey();
                    String fileName = file.getName();
                    try {
                        MediaType type = MediaType.parse(Files.probeContentType(file.toPath()));
                        builder.addFormDataPart(key, fileName, RequestBody.create(file, type));
                    } catch (IOException e) {
                        throw new RuntimeException(String.format("add file(%s) exception", fileName), e);
                    }
                    sbd.append("<").append(key).append(" : ").append(file.getPath()).append(">");
                }
            }
            sbd.append(")");
        }

        Request.Builder request;
        if ("PUT".equalsIgnoreCase(method)) {
            request = new Request.Builder().put(builder.build());
        } else {
            request = new Request.Builder().post(builder.build());
        }
        handleHeader(request, HttpConst.handleContentType(headers));
        return handleRequest(url, request, String.format("upload file[%s]", sbd));
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
    private static ResponseData handleRequest(String url, Request.Builder builder, String params) {
        String traceId = LogUtil.getTraceId();
        if (U.isNotBlank(traceId)) {
            builder.header(Const.TRACE, traceId);
        }
        String language = LogUtil.getLanguage();
        if (U.isNotBlank(language)) {
            builder.header("Accept-Language", language);
        }
        url = HttpConst.handleEmptyScheme(url);
        Request request = builder.header("User-Agent", USER_AGENT).url(url).build();
        String method = request.method();
        Headers reqHeaders = request.headers();
        Headers resHeaders = null;
        Integer responseCode = null;
        String statusCode = "";
        String result = "";
        long start = System.currentTimeMillis();
        try (
                Response response = HTTP_CLIENT.newCall(request).execute();
                ResponseBody body = response.body()
        ) {
            resHeaders = response.headers();
            responseCode = response.code();
            statusCode = responseCode + " ";
            result = U.isNotNull(body) ? body.string() : null;
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(collectContext(start, method, url, params, reqHeaders, statusCode, resHeaders, result));
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(collectContext(start, method, url, params, reqHeaders, statusCode, resHeaders, result), e);
            }
        }
        return new ResponseData(responseCode, result);
    }
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
        boolean hasReqHeader = U.isNotNull(reqHeaders);
        if (hasReqHeader) {
            sbd.append("header(");
            for (String key : reqHeaders.names()) {
                String value = reqHeaders.get(key);
                sbd.append("<").append(key).append(" : ").append(DesensitizationUtil.desByKey(key, value)).append(">");
            }
            sbd.append(")");
        }
        boolean hasParam = U.isNotBlank(params);
        if (hasParam) {
            if (hasReqHeader) {
                sbd.append(" ");
            }
            sbd.append("param(").append(U.compress(params)).append(")");
        }
        sbd.append("], res[").append(statusCode);
        boolean hasResHeader = U.isNotNull(resHeaders);
        if (hasResHeader) {
            sbd.append(" header(");
            for (String key : resHeaders.names()) {
                String value = resHeaders.get(key);
                sbd.append("<").append(key).append(" : ").append(DesensitizationUtil.desByKey(key, value)).append(">");
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
