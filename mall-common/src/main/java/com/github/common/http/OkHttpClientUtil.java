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
import java.util.*;
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
            // .followRedirects(true) // 默认就会处理重定向
            // .followSslRedirects(true) // 默认就会处理 ssl 的重定向
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
        String useUrl = HttpConst.appendParamsToUrl(url, params);
        Request.Builder builder = new Request.Builder();
        handleHeader(builder, HttpConst.handleContentType(headers, false));
        return handleRequest(useUrl, builder, null, null);
    }


    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static ResponseData postWithFormUrlEncode(String url, Map<String, Object> params) {
        return postWithFormUrlEncode(url, params, null);
    }
    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static ResponseData postWithFormUrlEncode(String url, Map<String, Object> params, Map<String, Object> headers) {
        RequestBody requestBody = RequestBody.create(U.formatParam(false, true, params), FORM);
        Request.Builder builder = new Request.Builder().post(requestBody);
        handleHeader(builder, HttpConst.handleContentType(headers, false));
        return handleRequest(url, builder, U.formatParam(params), null);
    }

    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithBody(String url, String json) {
        return postWithBody(url, null, json, null);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithBody(String url, Map<String, Object> params, String json) {
        return postWithBody(url, params, json, null);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithBody(String url, String json, Map<String, Object> headers) {
        return postWithBody(url, null, json, headers);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithBody(String url, Map<String, Object> params, String json, Map<String, Object> headers) {
        String content = U.toStr(json);
        String useUrl = HttpConst.appendParamsToUrl(url, params);
        Request.Builder builder = new Request.Builder().post(RequestBody.create(content, JSON));
        handleHeader(builder, HttpConst.handleContentType(headers, true));
        return handleRequest(useUrl, builder, null, content);
    }

    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithXmlBody(String url, String xml) {
        return postWithXmlBody(url, null, xml, null);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithXmlBody(String url, Map<String, Object> params, String xml) {
        return postWithXmlBody(url, params, xml, null);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithXmlBody(String url, String xml, Map<String, Object> headers) {
        return postWithXmlBody(url, null, xml, headers);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithXmlBody(String url, Map<String, Object> params, String xml, Map<String, Object> headers) {
        String content = U.toStr(xml);
        String useUrl = HttpConst.appendParamsToUrl(url, params);
        Request.Builder builder = new Request.Builder().post(RequestBody.create(content, JSON));
        handleHeader(builder, HttpConst.handleXml(headers));
        return handleRequest(useUrl, builder, null, content);
    }


    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static ResponseData put(String url, String json) {
        return put(url, null, json, null);
    }
    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static ResponseData put(String url, Map<String, Object> params, String json) {
        return put(url, params, json, null);
    }
    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static ResponseData put(String url, String json, Map<String, Object> headers) {
        return put(url, null, json, headers);
    }
    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static ResponseData put(String url, Map<String, Object> params, String json, Map<String, Object> headers) {
        String content = U.toStr(json);
        String useUrl = HttpConst.appendParamsToUrl(url, params);
        RequestBody requestBody = RequestBody.create(content, JSON);
        Request.Builder builder = new Request.Builder().put(requestBody);
        handleHeader(builder, HttpConst.handleContentType(headers, true));
        return handleRequest(useUrl, builder, null, content);
    }


    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static ResponseData delete(String url, String json) {
        return deleteWithHeader(url, json, null);
    }
    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static ResponseData deleteWithHeader(String url, String json, Map<String, Object> headers) {
        String content = U.toStr(json);
        RequestBody requestBody = RequestBody.create(content, JSON);
        Request.Builder builder = new Request.Builder().delete(requestBody);
        handleHeader(builder, HttpConst.handleContentType(headers, true));
        return handleRequest(url, builder, null, content);
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
        return handleRequest(url, request, String.format("upload file[%s]", sbd), null);
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
    private static ResponseData handleRequest(String url, Request.Builder builder, String printParams, String printJsonBody) {
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
                LogUtil.ROOT_LOG.info(collectContext(start, method, url, printParams,
                        printJsonBody, reqHeaders, statusCode, resHeaders, result));
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(collectContext(start, method, url, printParams,
                        printJsonBody, reqHeaders, statusCode, resHeaders, result), e);
            }
        }
        return new ResponseData(responseCode, handleResponseHeader(resHeaders), result);
    }
    private static Map<String, String> handleResponseHeader(Headers resHeaders) {
        if (U.isNotNull(resHeaders)) {
            Map<String, String> returnMap = new HashMap<>();
            for (String key : resHeaders.names()) {
                returnMap.put(key, resHeaders.get(key));
            }
            return returnMap;
        }
        return Collections.emptyMap();
    }
    private static String headerInfo(Headers headers) {
        List<String> list = new ArrayList<>();
        for (String key : headers.names()) {
            String value = headers.get(key);
            list.add("<" + key + " : " + DesensitizationUtil.desByKey(key, value) + ">");
        }
        return String.join("", list);
    }
    private static String collectContext(long start, String method, String url, String printParams, String printJsonBody,
                                         Headers reqHeaders, String statusCode, Headers resHeaders, String result) {
        StringBuilder sbd = new StringBuilder();
        long now = System.currentTimeMillis();
        sbd.append("OkHttp3 => [")
                .append(DateUtil.formatDateTimeMs(new Date(start))).append(" -> ")
                .append(DateUtil.formatDateTimeMs(new Date(now)))
                .append("(").append(DateUtil.toHuman(now - start)).append(")")
                .append("] (").append(method).append(" ").append(url).append(")");
        sbd.append(" req[");
        if (U.isNotNull(reqHeaders)) {
            sbd.append("header(").append(headerInfo(reqHeaders)).append(")");
        }
        if (U.isNotBlank(printParams)) {
            if (!sbd.toString().endsWith("[")) {
                sbd.append(" ");
            }
            sbd.append("param(").append(U.compress(printParams)).append(")");
        }
        if (U.isNotBlank(printJsonBody)) {
            if (!sbd.toString().endsWith("[")) {
                sbd.append(" ");
            }
            sbd.append("body(").append(U.compress(printJsonBody)).append(")");
        }
        sbd.append("], res[").append(statusCode);
        boolean hasResHeader = U.isNotNull(resHeaders);
        if (hasResHeader) {
            sbd.append(" header(").append(headerInfo(resHeaders)).append(")");
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
