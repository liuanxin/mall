package com.github.common.http;

import com.github.common.Const;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DuplicatedCode")
public class HttpUrlConnectionUtil {

    // private static final String USER_AGENT = HttpConst.getUserAgent("http_url_connection");
    private static final int MAX_REDIRECT_COUNT = 10;


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
        Map<String, Object> headerMap = U.defaultIfNull(headers, new HashMap<>());
        headerMap.put("Content-Type", "application/x-www-form-urlencoded");
        return handleRequest("GET", useUrl, timeoutSecond, null, headerMap, null, printLog);
    }


    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static HttpData postUrlEncode(String url, Map<String, Object> params) {
        return postUrlEncode(url, params, null);
    }
    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static HttpData postUrlEncode(String url, Map<String, Object> params, Map<String, Object> headers) {
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
        Map<String, Object> headerMap = U.defaultIfNull(headers, new HashMap<>());
        headerMap.put("Content-Type", "application/x-www-form-urlencoded");
        return handleRequest("POST", url, timeoutSecond, params, headerMap, null, printLog);
    }

    /** 向指定的 url 基于 post 发起请求(body 中是 json) */
    public static HttpData post(String url, String json) {
        return post(url, null, json);
    }
    /** 向指定的 url 基于 post 发起请求(body 中是 json) */
    public static HttpData post(String url, Map<String, Object> params, String json) {
        return post(url, params, json, A.maps("Content-Type", "application/json"));
    }
    /** 向指定的 url 基于 post 发起请求(json : data 是 json 格式 + header 中的 Content-Type 是 application/json. xml : data 是 xml 格式 + header 中 Content-Type 是 application/xml) */
    public static HttpData post(String url, String data, Map<String, Object> headers) {
        return post(url, null, data, headers);
    }
    /** 向指定的 url 基于 post 发起请求(json : data 是 json 格式 + header 中的 Content-Type 是 application/json. xml : data 是 xml 格式 + header 中 Content-Type 是 application/xml) */
    public static HttpData post(String url, Map<String, Object> params, String data, Map<String, Object> headers) {
        return post(url, params, data, headers, true);
    }
    /** 向指定的 url 基于 post 发起请求(json : data 是 json 格式 + header 中的 Content-Type 是 application/json. xml : data 是 xml 格式 + header 中 Content-Type 是 application/xml) */
    public static HttpData post(String url, Map<String, Object> params, String data,
                                Map<String, Object> headers, boolean printLog) {
        return post(url, 0, params, data, headers, printLog);
    }
    /** 向指定的 url 基于 post 发起请求(json : data 是 json 格式 + header 中的 Content-Type 是 application/json. xml : data 是 xml 格式 + header 中 Content-Type 是 application/xml) */
    public static HttpData post(String url, int timeoutSecond, Map<String, Object> params,
                                String data, Map<String, Object> headers, boolean printLog) {
        String content = U.toStr(data);
        String useUrl = HttpConst.appendParamsToUrl(url, params);
        return handleRequest("POST", useUrl, timeoutSecond, null, headers, content, printLog);
    }


    /** 向指定的 url 基于 put 发起请求(body 中是 json) */
    public static HttpData put(String url, String json) {
        return put(url, null, json);
    }
    /** 向指定的 url 基于 put 发起请求(body 中是 json) */
    public static HttpData put(String url, Map<String, Object> params, String json) {
        return put(url, params, json, A.maps("Content-Type", "application/json"));
    }
    /** 向指定的 url 基于 put 发起请求(json : data 是 json 格式 + header 中的 Content-Type 是 application/json. xml : data 是 xml 格式 + header 中 Content-Type 是 application/xml) */
    public static HttpData put(String url, Map<String, Object> params, String data, Map<String, Object> headers) {
        return put(url, params, headers, data, true);
    }
    /** 向指定的 url 基于 put 发起请求(json : data 是 json 格式 + header 中的 Content-Type 是 application/json. xml : data 是 xml 格式 + header 中 Content-Type 是 application/xml) */
    public static HttpData put(String url, Map<String, Object> params, Map<String, Object> headers,
                               String data, boolean printLog) {
        return put(url, 0, params, headers, data, printLog);
    }
    /** 向指定的 url 基于 put 发起请求(json : data 是 json 格式 + header 中的 Content-Type 是 application/json. xml : data 是 xml 格式 + header 中 Content-Type 是 application/xml) */
    public static HttpData put(String url, int timeoutSecond, Map<String, Object> params,
                               Map<String, Object> headers, String data, boolean printLog) {
        String content = U.toStr(data);
        String useUrl = HttpConst.appendParamsToUrl(url, params);
        return handleRequest("PUT", useUrl, timeoutSecond, null, headers, content, printLog);
    }


    /** 向指定的 url 基于 delete 发起请求(body 中是 json) */
    public static HttpData delete(String url, String json) {
        return delete(url, json, A.maps("Content-Type", "application/json"));
    }
    /** 向指定的 url 基于 delete 发起请求(json : data 是 json 格式 + header 中的 Content-Type 是 application/json. xml : data 是 xml 格式 + header 中 Content-Type 是 application/xml) */
    public static HttpData delete(String url, String data, Map<String, Object> headers) {
        return delete(url, data, headers, true);
    }
    /** 向指定的 url 基于 delete 发起请求(json : data 是 json 格式 + header 中的 Content-Type 是 application/json. xml : data 是 xml 格式 + header 中 Content-Type 是 application/xml) */
    public static HttpData delete(String url, String data, Map<String, Object> headers, boolean printLog) {
        return delete(url, 0, data, headers, printLog);
    }
    /** 向指定的 url 基于 delete 发起请求(json : data 是 json 格式 + header 中的 Content-Type 是 application/json. xml : data 是 xml 格式 + header 中 Content-Type 是 application/xml) */
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
        HttpData httpData = new HttpData();
        HttpURLConnection con = null;
        try {
            url = HttpConst.handleEmptyScheme(url);
            if (url.startsWith("https://")) {
                HttpsURLConnection.setDefaultSSLSocketFactory(TrustCerts.IGNORE_SSL
                        ? TrustCerts.IGNORE_SSL_FACTORY
                        : HttpsURLConnection.getDefaultSSLSocketFactory());
            }
            con = (HttpURLConnection) new URL(url).openConnection();
            String useMethod = "PUT".equalsIgnoreCase(method) ? "PUT" : "POST";
            con.setRequestMethod(useMethod);
            con.setConnectTimeout(HttpConst.CONNECT_TIME_OUT);
            con.setReadTimeout(timeoutSecond > 0 ? ((int) TimeUnit.SECONDS.toMillis(timeoutSecond)) : HttpConst.READ_TIME_OUT);
            if (A.isNotEmpty(headers)) {
                for (Map.Entry<String, ?> entry : headers.entrySet()) {
                    con.setRequestProperty(entry.getKey(), U.toStr(entry.getValue()));
                }
            }
            // con.setRequestProperty("User-Agent", USER_AGENT);
            String traceId = LogUtil.getTraceId();
            if (U.isNotBlank(traceId)) {
                con.setRequestProperty(Const.TRACE, traceId);
            }
            String language = LogUtil.getLanguage();
            if (U.isNotBlank(language)) {
                con.setRequestProperty("Accept-Language", language);
            }
            Map<String, String> fileMap = new LinkedHashMap<>();
            boolean hasParam = A.isNotEmpty(params);
            boolean hasFile = A.isNotEmpty(files);
            if (hasParam || hasFile) {
                con.setDoOutput(true);

                String boundary = U.uuid16();
                // 使用 from 表单上传文件时, 需设置 enctype="multipart/form-data" 属性
                con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                try (DataOutputStream data = new DataOutputStream(con.getOutputStream())) {
                    if (hasParam) {
                        for (Map.Entry<String, Object> entry : params.entrySet()) {
                            String key = entry.getKey();
                            String value = U.toStr(entry.getValue());

                            data.writeBytes("--" + boundary + "\r\n");
                            String paramInfo = "Content-Disposition: form-data; name=\"%s\"\r\n\r\n";
                            data.writeBytes(String.format(paramInfo, key));
                            data.writeBytes(value + "\r\n");
                        }
                    }
                    if (hasFile) {
                        for (Map.Entry<String, File> entry : files.entrySet()) {
                            String key = entry.getKey();
                            File file = entry.getValue();

                            data.writeBytes("--" + boundary + "\r\n");
                            String paramInfo = "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n\r\n";
                            data.writeBytes(String.format(paramInfo, key, file.getName()));
                            data.write(Files.readAllBytes(file.toPath()));
                            data.writeBytes("\r\n");
                            fileMap.put(key, file.toString());
                        }
                    }
                    data.writeBytes("--" + boundary + "--\r\n");
                    data.flush();
                }
            }

            Map<String, Object> reqHeader = handleHeader(con.getRequestProperties());
            httpData.fillReq(method, url, reqHeader, U.formatPrintParam(params), JsonUtil.toJsonNil(fileMap));
            con.connect();

            int resCode = con.getResponseCode();
            String result;
            try (
                    InputStream input = con.getInputStream();
                    InputStreamReader in = new InputStreamReader(input);
                    BufferedReader reader = new BufferedReader(in)
            ) {
                StringBuilder sbd = new StringBuilder();
                for (String line; (line = reader.readLine()) != null;) {
                    sbd.append(line);
                }
                result = sbd.toString();
            }
            Map<String, List<String>> resHeaders = con.getHeaderFields();
            // ??? null -> HTTP/1.1 200 OK
            String nilInfo = A.first(resHeaders.get(null));
            if (U.isNotBlank(result) && U.isNotBlank(nilInfo)) {
                if (result.endsWith(nilInfo)) {
                    result = result.substring(0, result.length() - nilInfo.length());
                } else if (result.startsWith(nilInfo)) {
                    result = result.substring(nilInfo.length());
                }
            }
            httpData.fillRes(resCode, handleHeader(resHeaders), result);
            if (printLog && LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(httpData.toString());
            }
        } catch (Exception e) {
            httpData.fillException(e);
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(httpData.toString(), e);
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return httpData;
    }

    private static HttpData handleRequest(String method, String url, int timeoutSecond, Map<String, Object> params,
                                          Map<String, Object> headers, String body, boolean printLog) {
        HttpData httpData = new HttpData();
        HttpURLConnection con = null;
        try {
            if (url.toLowerCase().startsWith("https://")) {
                HttpsURLConnection.setDefaultSSLSocketFactory(TrustCerts.IGNORE_SSL
                        ? TrustCerts.IGNORE_SSL_FACTORY : HttpsURLConnection.getDefaultSSLSocketFactory());
            }
            List<String> redirectUrlList = new ArrayList<>();
            String connectionUrl = HttpConst.handleEmptyScheme(url);
            redirectUrlList.add(connectionUrl);
            Map<String, List<String>> resHeaders = null;
            for (int i = 0; i < MAX_REDIRECT_COUNT; i++) {
                try {
                    con = (HttpURLConnection) new URL(connectionUrl).openConnection();
                    con.setRequestMethod(method);
                    con.setConnectTimeout(HttpConst.CONNECT_TIME_OUT);
                    con.setReadTimeout(timeoutSecond > 0 ? ((int) TimeUnit.SECONDS.toMillis(timeoutSecond)) : HttpConst.READ_TIME_OUT);
                    if (A.isNotEmpty(headers)) {
                        for (Map.Entry<String, ?> entry : headers.entrySet()) {
                            con.setRequestProperty(entry.getKey(), U.toStr(entry.getValue()));
                        }
                    }
                    // con.setRequestProperty("User-Agent", USER_AGENT);
                    String traceId = LogUtil.getTraceId();
                    if (U.isNotBlank(traceId)) {
                        con.setRequestProperty(Const.TRACE, traceId);
                    }
                    if (A.isNotEmpty(params) || U.isNotBlank(body)) {
                        // 默认值 false, 当向远程服务器传送数据/写数据时, 需设置为 true
                        con.setDoOutput(true);
                        try (OutputStreamWriter output = new OutputStreamWriter(con.getOutputStream())) {
                            output.write(U.defaultIfBlank(U.formatSendParam(params), body));
                            output.flush();
                        }
                    }

                    Map<String, Object> reqHeader = handleHeader(con.getRequestProperties());
                    // 这里的 url 可能是重定向之后的
                    httpData.fillReq(method, connectionUrl, reqHeader, U.formatPrintParam(params), body);
                    con.connect();

                    int responseCode = con.getResponseCode();
                    resHeaders = con.getHeaderFields();
                    if (String.valueOf(responseCode).startsWith("30")) {
                        // 30x 自动进行重定向
                        connectionUrl = URLDecoder.decode(con.getHeaderField("Location"), StandardCharsets.UTF_8);
                        redirectUrlList.add(connectionUrl);
                    } else {
                        String result;
                        try (
                                InputStream input = con.getInputStream();
                                InputStreamReader in = new InputStreamReader(input);
                                BufferedReader reader = new BufferedReader(in)
                        ) {
                            StringBuilder sbd = new StringBuilder();
                            for (String line; (line = reader.readLine()) != null;) {
                                sbd.append(line);
                            }
                            result = sbd.toString();
                        }
                        // null : HTTP/1.1 200 OK
                        String nilInfo = A.first(resHeaders.get(null));
                        if (U.isNotBlank(result) && U.isNotBlank(nilInfo)) {
                            if (result.endsWith(nilInfo)) {
                                result = result.substring(0, result.length() - nilInfo.length());
                            } else if (result.startsWith(nilInfo)) {
                                result = result.substring(nilInfo.length());
                            }
                        }
                        httpData.fillRes(responseCode, handleHeader(resHeaders), result);
                        if (printLog && LogUtil.ROOT_LOG.isInfoEnabled()) {
                            LogUtil.ROOT_LOG.info(httpData.toString());
                        }
                        return httpData;
                    }
                } catch (IOException e) {
                    httpData.fillException(e);
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error(httpData.toString(), e);
                    }
                    return httpData;
                }
            }
            httpData.fillRes(302, handleHeader(resHeaders), JsonUtil.toJson(A.maps(
                    "error", "too_many_redirects",
                    "redirect_chain", redirectUrlList
            )));
            return httpData;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }
    private static Map<String, Object> handleHeader(Map<String, List<String>> resHeaders) {
        if (A.isNotEmpty(resHeaders)) {
            Map<String, Object> returnMap = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : resHeaders.entrySet()) {
                returnMap.put(entry.getKey(), String.join(",", entry.getValue()));
            }
            return returnMap;
        }
        return Collections.emptyMap();
    }
}
