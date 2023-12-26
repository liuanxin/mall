package com.github.common.http;

import com.github.common.Const;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.AsyncUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.U;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("DuplicatedCode")
public class HttpClientUtil {

    private static final String USER_AGENT = HttpConst.getUserAgent("http_client");

    private static final HttpClient HTTP_CLIENT;
    static {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .executor(AsyncUtil.ioExecutor())
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofMillis(HttpConst.CONNECT_TIME_OUT));
        if (TrustCerts.IGNORE_SSL) {
            builder.sslContext(TrustCerts.IGNORE_SSL_CONTEXT);
        }
        HTTP_CLIENT = builder.build();
    }


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
        return handleRequest("GET", useUrl, timeoutSecond, null, headers, null, printLog);
    }


    /** 向指定的 url 进行 post 请求(表单) */
    public static HttpData postUrlEncode(String url, Map<String, Object> params) {
        return postUrlEncode(url, params, null);
    }
    /** 向指定的 url 进行 post 请求(表单) */
    public static HttpData postUrlEncode(String url, Map<String, Object> params, Map<String, Object> headers) {
        return postUrlEncode(url, params, headers, true);
    }
    /** 向指定的 url 进行 post 请求(表单) */
    public static HttpData postUrlEncode(String url, Map<String, Object> params,
                                         Map<String, Object> headers, boolean printLog) {
        return postUrlEncode(url, 0, params, headers, printLog);
    }
    /** 向指定的 url 进行 post 请求(表单) */
    public static HttpData postUrlEncode(String url, int timeoutSecond, Map<String, Object> params,
                                         Map<String, Object> headers, boolean printLog) {
        return handleRequest("POST", url, timeoutSecond, params, headers, null, printLog);
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
        String useUrl = HttpConst.appendParamsToUrl(url, params);
        return handleRequest("POST", useUrl, timeoutSecond, null, headers, json, printLog);
    }


    /** 向指定的 url 基于 put 发起请求 */
    public static HttpData put(String url, String data) {
        return put(url, null, data, null);
    }
    /** 向指定的 url 基于 put 发起请求 */
    public static HttpData put(String url, Map<String, Object> params, String data) {
        return put(url, params, data, null);
    }
    /** 向指定的 url 基于 put 发起请求 */
    public static HttpData put(String url, String data, Map<String, Object> headers) {
        return put(url, null, data, headers);
    }
    /** 向指定的 url 基于 put 发起请求 */
    public static HttpData put(String url, Map<String, Object> params, String data, Map<String, Object> headers) {
        return put(url, params, data, headers, true);
    }
    /** 向指定的 url 基于 put 发起请求 */
    public static HttpData put(String url, Map<String, Object> params, String data,
                               Map<String, Object> headers, boolean printLog) {
        return put(url, 0, params, data, headers, printLog);
    }
    /** 向指定的 url 基于 put 发起请求 */
    public static HttpData put(String url, int timeoutSecond, Map<String, Object> params, String data,
                               Map<String, Object> headers, boolean printLog) {
        String useUrl = HttpConst.appendParamsToUrl(url, params);
        return handleRequest("PUT", useUrl, timeoutSecond, null, headers, data, printLog);
    }


    /** 向指定的 url 基于 delete 发起请求 */
    public static HttpData delete(String url, String data) {
        return delete(url, data, null);
    }
    /** 向指定的 url 基于 delete 发起请求 */
    public static HttpData delete(String url, String data, Map<String, Object> headers) {
        return delete(url, data, headers, true);
    }
    /** 向指定的 url 基于 delete 发起请求 */
    public static HttpData delete(String url, String data, Map<String, Object> headers, boolean printLog) {
        return delete(url, 0, data, headers, printLog);
    }
    /** 向指定的 url 基于 delete 发起请求 */
    public static HttpData delete(String url, int timeoutSecond, String data,
                                  Map<String, Object> headers, boolean printLog) {
        return handleRequest("DELETE", url, timeoutSecond, null, headers, data, printLog);
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
    /** 向指定 url 上传文件, 只支持 POST|PUT(默认是 POST) + form-data 的方式 */
    public static HttpData uploadFile(String url, String method, Map<String, Object> headers,
                                      Map<String, Object> params, Map<String, File> files) {
        return uploadFile(url, method, 0, headers, params, files, true);
    }
    /** 向指定 url 上传文件, 只支持 POST|PUT(默认是 POST) + form-data 的方式 */
    public static HttpData uploadFile(String url, String method, int timeoutSecond, Map<String, Object> headers,
                                      Map<String, Object> params, Map<String, File> files, boolean printLog) {
        String useMethod = "PUT".equalsIgnoreCase(method) ? "PUT" : "POST";

        Map<String, String> fileMap = new LinkedHashMap<>();
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        boolean hasParam = A.isNotEmpty(params);
        boolean hasFile = A.isNotEmpty(files);
        HttpRequest.BodyPublisher body;
        if (hasParam || hasFile) {
            String boundary = U.uuid16();
            builder.setHeader("Content-Type", "multipart/form-data;boundary=" + boundary);
            List<byte[]> arr = new ArrayList<>();
            if (hasParam) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    String key = entry.getKey();
                    String value = U.toStr(entry.getValue());

                    arr.add(handleBytes("--" + boundary + "\r\n"));
                    String paramInfo = "Content-Disposition: form-data; name=\"%s\"\r\n\r\n";
                    arr.add(handleBytes(String.format(paramInfo, key)));
                    arr.add(handleBytes(value + "\r\n"));
                }
            }
            if (hasFile) {
                for (Map.Entry<String, File> entry : files.entrySet()) {
                    String key = entry.getKey();
                    File file = entry.getValue();

                    arr.add(handleBytes("--" + boundary + "\r\n"));
                    String paramInfo = "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n\r\n";
                    arr.add(handleBytes(String.format(paramInfo, key, file.getName())));
                    try {
                        arr.add(Files.readAllBytes(file.toPath()));
                    } catch (IOException e) {
                        throw new RuntimeException("read file exception", e);
                    }
                    arr.add(handleBytes("\r\n"));

                    fileMap.put(key, file.toString());
                }
            }
            arr.add(handleBytes("--" + boundary + "--"));
            body = HttpRequest.BodyPublishers.ofByteArrays(arr);
        } else {
            body = HttpRequest.BodyPublishers.noBody();
        }
        builder.method(useMethod, body);
        builder.uri(URI.create(HttpConst.handleEmptyScheme(url)));
        if (A.isNotEmpty(headers)) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                builder.setHeader(entry.getKey(), U.toStr(entry.getValue()));
            }
        }
        builder.setHeader("User-Agent", USER_AGENT);
        builder.timeout(timeoutSecond > 0 ? Duration.ofSeconds(timeoutSecond) : Duration.ofMillis(HttpConst.READ_TIME_OUT));
        HttpRequest request = builder.build();

        HttpData httpData = new HttpData();
        httpData.fillReq(useMethod, url, handleHeader(request.headers()), U.formatPrintParam(params), JsonUtil.toJsonNil(fileMap));
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            httpData.fillRes(response.statusCode(), handleHeader(response.headers()), response.body());
            if (printLog && LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(httpData.toString());
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(httpData.toString(), e);
            }
            httpData.fillException(e);
        }
        return httpData;
    }

    private static byte[] handleBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    private static HttpData handleRequest(String method, String url, int timeoutSecond, Map<String, Object> params,
                                          Map<String, Object> headers, String data, boolean printLog) {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        HttpRequest.BodyPublisher body;
        if (A.isNotEmpty(params)) {
            body = HttpRequest.BodyPublishers.ofString(U.formatSendParam(params));
        } else {
            body = U.isBlank(data) ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(data);
        }
        builder.method(method, body);
        builder.uri(URI.create(HttpConst.handleEmptyScheme(url)));
        builder.timeout(timeoutSecond > 0 ? Duration.ofSeconds(timeoutSecond) : Duration.ofMillis(HttpConst.READ_TIME_OUT));
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
        HttpData httpData = new HttpData();
        httpData.fillReq(method, url, handleHeader(request.headers()), U.formatPrintParam(params), data);
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            httpData.fillRes(response.statusCode(), handleHeader(response.headers()), response.body());
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
    private static Map<String, Object> handleHeader(HttpHeaders headers) {
        Map<String, Object> header = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.map().entrySet()) {
            String key = entry.getKey();
            String value = String.join(",", entry.getValue());
            header.put(key, value);
        }
        return header;
    }
}
