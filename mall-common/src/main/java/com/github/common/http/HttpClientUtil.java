package com.github.common.http;

import com.github.common.Const;
import com.github.common.date.DateUtil;
import com.github.common.util.*;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@SuppressWarnings("DuplicatedCode")
public class HttpClientUtil {

    private static final String USER_AGENT = HttpConst.getUserAgent("http_client");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .executor(AsyncUtil.ioExecutor())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofMillis(HttpConst.CONNECT_TIME_OUT))
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
        Map<String, Object> headerMap = HttpConst.handleContentType(headers, false);
        return handleRequest("GET", HttpConst.appendParamsToUrl(url, params), null, null, headerMap);
    }


    /** 向指定的 url 进行 post 请求(表单) */
    public static ResponseData postWithForm(String url, Map<String, Object> params) {
        return postWithForm(url, params, null);
    }
    /** 向指定的 url 进行 post 请求(表单) */
    public static ResponseData postWithForm(String url, Map<String, Object> params, Map<String, Object> headers) {
        Map<String, Object> headerMap = HttpConst.handleContentType(headers, false);
        return handleRequest("POST", url, U.formatParam(false, params), U.formatParam(true, params), headerMap);
    }

    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithBody(String url, String data) {
        return postWithBody(url, data, null);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithBody(String url, String data, Map<String, Object> headers) {
        Map<String, Object> headerMap = HttpConst.handleContentType(headers, true);
        return handleRequest("POST", url, U.toStr(data), data, headerMap);
    }


    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static ResponseData put(String url, String data) {
        return put(url, data, null);
    }
    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static ResponseData put(String url, String data, Map<String, Object> headers) {
        Map<String, Object> headerMap = HttpConst.handleContentType(headers, true);
        return handleRequest("PUT", url, U.toStr(data), data, headerMap);
    }


    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static ResponseData delete(String url, String json) {
        return deleteWithHeader(url, json, null);
    }
    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static ResponseData deleteWithHeader(String url, String json, Map<String, Object> headers) {
        Map<String, Object> headerMap = HttpConst.handleContentType(headers, true);
        return handleRequest("DELETE", url, U.toStr(json), json, headerMap);
    }

    /** 向指定 url 上传文件(基于 POST + form-data 的方式) */
    public static ResponseData uploadFile(String url, Map<String, Object> headers, Map<String, Object> params, Map<String, File> files) {
        return uploadFile(url, null, headers, params, files);
    }
    /** 向指定 url 上传文件, 只支持 POST|PUT(默认是 POST) + form-data 的方式 */
    public static ResponseData uploadFile(String url, String method, Map<String, Object> headers,
                                          Map<String, Object> params, Map<String, File> files) {
        long start = System.currentTimeMillis();
        String useMethod = "PUT".equalsIgnoreCase(method) ? "PUT" : "POST";
        Map<String, List<String>> reqHeaders = null;
        StringBuilder sbd = new StringBuilder();
        Integer responseCode = null;
        String resCode = "";
        Map<String, List<String>> resHeaders = null;
        String result = null;

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder();
            boolean hasParam = A.isNotEmpty(params);
            boolean hasFile = A.isNotEmpty(files);
            HttpRequest.BodyPublisher body;
            if (hasParam || hasFile) {
                String boundary = U.uuid16();
                builder.setHeader("Content-Type", "multipart/form-data;boundary=" + boundary);
                List<byte[]> arr = new ArrayList<>();
                if (hasParam) {
                    sbd.append("param(");
                    for (Map.Entry<String, Object> entry : params.entrySet()) {
                        String key = entry.getKey();
                        String value = U.toStr(entry.getValue());

                        arr.add(handleBytes("--" + boundary + "\r\n"));
                        String paramInfo = "Content-Disposition: form-data; name=\"%s\"\r\n\r\n";
                        arr.add(handleBytes(String.format(paramInfo, key)));
                        arr.add(handleBytes(value + "\r\n"));

                        sbd.append("<").append(key).append(" : ").append(DesensitizationUtil.desByKey(key, value)).append(">");
                    }
                    sbd.append(")");
                }
                if (hasParam && hasFile) {
                    sbd.append(" ");
                }
                if (hasFile) {
                    sbd.append("file(");
                    for (Map.Entry<String, File> entry : files.entrySet()) {
                        String key = entry.getKey();
                        File file = entry.getValue();

                        arr.add(handleBytes("--" + boundary + "\r\n"));
                        String paramInfo = "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n\r\n";
                        arr.add(handleBytes(String.format(paramInfo, key, file.getName())));
                        arr.add(Files.readAllBytes(file.toPath()));
                        arr.add(handleBytes("\r\n"));

                        sbd.append("<").append(key).append(" : ").append(file).append(">");
                    }
                    sbd.append(")");
                }
                arr.add(handleBytes("--" + boundary + "--"));
                body = HttpRequest.BodyPublishers.ofByteArrays(arr);
            } else {
                body = HttpRequest.BodyPublishers.noBody();
            }
            builder.method(useMethod, body);
            url = HttpConst.handleEmptyScheme(url);
            builder.uri(URI.create(url));
            Map<String, Object> headerMap = HttpConst.handleContentType(headers);
            if (A.isNotEmpty(headerMap)) {
                for (Map.Entry<String, Object> entry : headerMap.entrySet()) {
                    builder.setHeader(entry.getKey(), U.toStr(entry.getValue()));
                }
            }
            builder.setHeader("User-Agent", USER_AGENT);
            HttpRequest request = builder.build();
            reqHeaders = request.headers().map();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            responseCode = response.statusCode();
            resCode = responseCode + " ";
            resHeaders = response.headers().map();
            result = response.body();
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                String print = String.format("upload file[%s]", sbd);
                LogUtil.ROOT_LOG.info(collectContext(start, useMethod, url, print, reqHeaders, resCode, resHeaders, result));
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                String print = String.format("upload file[%s]", sbd);
                LogUtil.ROOT_LOG.error(collectContext(start, useMethod, url, print, reqHeaders, resCode, resHeaders, result), e);
            }
        }
        return new ResponseData(responseCode, result);
    }

    private static byte[] handleBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    private static ResponseData handleRequest(String method, String url, String data, String printData, Map<String, Object> headers) {
        long start = System.currentTimeMillis();
        printData = U.defaultIfBlank(printData, data);
        Map<String, List<String>> reqHeaders = null;
        Integer responseCode = null;
        String resCode = "";
        Map<String, List<String>> resHeaders = null;
        String result = null;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder();
            builder.method(method, U.isBlank(data) ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(data));
            url = HttpConst.handleEmptyScheme(url);
            builder.uri(URI.create(url));
            builder.timeout(Duration.ofMillis(HttpConst.READ_TIME_OUT));
            if (A.isNotEmpty(headers)) {
                for (Map.Entry<String, Object> entry : headers.entrySet()) {
                    builder.setHeader(entry.getKey(), U.toStr(entry.getValue()));
                }
            }
            builder.setHeader("User-Agent", USER_AGENT);
            String traceId = LogUtil.getTraceId();
            if (U.isNotBlank(traceId)) {
                builder.header(Const.TRACE, traceId);
            }
            String language = LogUtil.getLanguage();
            if (U.isNotBlank(language)) {
                builder.header("Accept-Language", language);
            }
            HttpRequest request = builder.build();
            reqHeaders = request.headers().map();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            responseCode = response.statusCode();
            resCode = responseCode + " ";
            resHeaders = response.headers().map();
            result = response.body();
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(collectContext(start, method, url, printData, reqHeaders, resCode, resHeaders, result));
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(collectContext(start, method, url, printData, reqHeaders, resCode, resHeaders, result), e);
            }
        }
        return new ResponseData(responseCode, result);
    }
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
        boolean hasReqHeader = A.isNotEmpty(reqHeaders);
        if (hasReqHeader) {
            sbd.append("header(");
            for (Map.Entry<String, List<String>> entry : reqHeaders.entrySet()) {
                String key = entry.getKey();
                String value = String.join(",", entry.getValue());
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
        sbd.append("], res[").append(resCode);
        boolean hasResHeader = A.isNotEmpty(resHeaders);
        if (hasResHeader) {
            sbd.append("header(");
            for (Map.Entry<String, List<String>> entry : resHeaders.entrySet()) {
                String key = entry.getKey();
                String value = String.join(",", entry.getValue());
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
