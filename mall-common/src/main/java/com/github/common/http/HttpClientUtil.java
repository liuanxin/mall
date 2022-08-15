package com.github.common.http;

import com.github.common.date.DateUtil;
import com.github.common.util.*;
import com.google.common.base.Joiner;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DuplicatedCode")
public class HttpClientUtil {

    private static final String USER_AGENT = HttpConst.getUserAgent("http_client");

    private static final Executor EXECUTOR = new ThreadPoolExecutor(
            Math.max(U.PROCESSORS - 1, 1),
            (U.PROCESSORS + 2),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(U.PROCESSORS << 6),
            AsyncUtil.wrapThreadFactory()
    );

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .executor(EXECUTOR)
            .connectTimeout(Duration.ofMillis(HttpConst.CONNECT_TIME_OUT))
            .build();


    /** 向指定 url 进行 get 请求 */
    public static String get(String url) {
        return get(url, null);
    }
    /** 向指定 url 进行 get 请求 */
    public static String get(String url, Map<String, Object> params) {
        return getWithHeader(url, params, null);
    }
    /** 向指定 url 进行 get 请求 */
    public static String getWithHeader(String url, Map<String, Object> params, Map<String, Object> headerMap) {
        return handleRequest("GET", HttpConst.handleGetParams(url, params), null, null, headerMap);
    }


    /** 向指定的 url 进行 post 请求(表单) */
    public static String post(String url, Map<String, Object> params) {
        return postWithHeader(url, params, null);
    }
    /** 向指定的 url 进行 post 请求(表单) */
    public static String postWithHeader(String url, Map<String, Object> params, Map<String, Object> headers) {
        // Content-Type 不设置则默认是 application/x-www-form-urlencoded
        return handleRequest("POST", url, U.formatParam(false, params), U.formatParam(true, params), headers);
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
        return handleRequest("POST", url, U.toStr(json), json, headerMap);
    }


    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static String put(String url, String json) {
        return putWithHeader(url, json, null);
    }
    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static String putWithHeader(String url, String json, Map<String, Object> headers) {
        Map<String, Object> headerMap = new LinkedHashMap<>();
        if (A.isNotEmpty(headers)) {
            headerMap.putAll(headers);
        }
        headerMap.put("Content-Type", "application/json");
        return handleRequest("PUT", url, U.toStr(json), json, headerMap);
    }


    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static String delete(String url, String json) {
        return deleteWithHeader(url, json, null);
    }
    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static String deleteWithHeader(String url, String json, Map<String, Object> headers) {
        Map<String, Object> headerMap = new LinkedHashMap<>();
        if (A.isNotEmpty(headers)) {
            headerMap.putAll(headers);
        }
        headerMap.put("Content-Type", "application/json");
        return handleRequest("DELETE", url, U.toStr(json), json, headerMap);
    }

    /** 向指定 url 上传文件 */
    public static String postFile(String url, Map<String, Object> headers, Map<String, Object> params, Map<String, File> files) {
        long start = System.currentTimeMillis();
        String method = "POST";
        Map<String, List<String>> reqHeaders = null;
        StringBuilder paramSbd = new StringBuilder();
        String resCode = "";
        Map<String, List<String>> resHeaders = null;
        String result = null;

        try {
            String boundary = "*****";
            boolean hasParam = A.isNotEmpty(params);
            boolean hasFile = A.isNotEmpty(files);
            HttpRequest.BodyPublisher body;
            if (hasParam || hasFile) {
                List<byte[]> arr = new ArrayList<>();
                if (hasParam) {
                    paramSbd.append("param(");
                    for (Map.Entry<String, Object> entry : params.entrySet()) {
                        String key = entry.getKey();
                        String value = U.toStr(entry.getValue());

                        arr.add(handleBytes("--" + boundary + "\r\n"));
                        arr.add(handleBytes("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n"));
                        arr.add(handleBytes(value + "\r\n"));

                        paramSbd.append("<").append(key).append(" : ")
                                .append(DesensitizationUtil.desByKey(key, value)).append(">");
                    }
                    paramSbd.append(")");
                }
                if (hasParam && hasFile) {
                    paramSbd.append(" ");
                }
                if (hasFile) {
                    paramSbd.append("file(");
                    for (Map.Entry<String, File> entry : files.entrySet()) {
                        String key = entry.getKey();
                        File file = entry.getValue();

                        arr.add(handleBytes("--" + boundary + "\r\n"));
                        arr.add(handleBytes("Content-Disposition: form-data; name=\"" + key
                                + "\";filename=\"" + file.getName() + "\"\r\n\r\n"));
                        arr.add(Files.readAllBytes(file.toPath()));
                        arr.add(handleBytes("\r\n"));

                        paramSbd.append("<").append(key).append(" : ").append(file).append(">");
                    }
                    paramSbd.append(")");
                }
                arr.add(handleBytes("--" + boundary + "--"));
                body = HttpRequest.BodyPublishers.ofByteArrays(arr);
            } else {
                body = HttpRequest.BodyPublishers.noBody();
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder();
            builder.method(method, body);
            url = HttpConst.handleEmptyScheme(url);
            builder.uri(URI.create(url));
            if (A.isNotEmpty(headers)) {
                for (Map.Entry<String, Object> entry : headers.entrySet()) {
                    builder.setHeader(entry.getKey(), U.toStr(entry.getValue()));
                }
            }
            builder.setHeader("User-Agent", USER_AGENT);
            builder.setHeader("Content-Type", "multipart/form-data;boundary=" + boundary);
            HttpRequest request = builder.build();
            reqHeaders = request.headers().map();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            resCode = response.statusCode() + " ";
            resHeaders = response.headers().map();
            result = response.body();
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                String print = String.format("upload file[%s]", paramSbd);
                LogUtil.ROOT_LOG.info(collectContext(start, method, url, print, reqHeaders, resCode, resHeaders, result));
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                String print = String.format("upload file[%s]", paramSbd);
                LogUtil.ROOT_LOG.error(collectContext(start, method, url, print, reqHeaders, resCode, resHeaders, result), e);
            }
        }
        return result;
    }

    private static byte[] handleBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    private static String handleRequest(String method, String url, String data, String printData, Map<String, Object> headers) {
        long start = System.currentTimeMillis();
        printData = U.defaultIfBlank(printData, data);
        Map<String, List<String>> reqHeaders = null;
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
            HttpRequest request = builder.build();
            reqHeaders = request.headers().map();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            resCode = response.statusCode() + " ";
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
        return result;
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
                sbd.append("<").append(entry.getKey()).append(" : ").append(Joiner.on(",").join(entry.getValue())).append(">");
            }
            sbd.append(")");
        }
        sbd.append("], res[").append(resCode);
        boolean hasResHeader = A.isNotEmpty(resHeaders);
        if (hasResHeader) {
            sbd.append("header(");
            for (Map.Entry<String, List<String>> entry : resHeaders.entrySet()) {
                sbd.append("<").append(entry.getKey()).append(" : ").append(Joiner.on(",").join(entry.getValue())).append(">");
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
