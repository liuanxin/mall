package com.github.common.http;

import com.github.common.Const;
import com.github.common.date.DateUtil;
import com.github.common.util.A;
import com.github.common.util.DesensitizationUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.U;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("DuplicatedCode")
public class HttpUrlConnectionUtil {

    private static final String USER_AGENT = HttpConst.getUserAgent("http_url_connection");


    /** 向指定 url 进行 get 请求 */
    public static ResponseData get(String url) {
        return get(url, null);
    }
    /** 向指定 url 进行 get 请求 */
    public static ResponseData get(String url, Map<String, Object> params) {
        return getWithHeader(url, params, null);
    }
    /** 向指定 url 进行 get 请求 */
    public static ResponseData getWithHeader(String url, Map<String, Object> params, Map<String, Object> headerMap) {
        return handleRequest("GET", HttpConst.handleGetParams(url, params), null, headerMap);
    }


    /** 向指定的 url 进行 post 请求(表单) */
    public static ResponseData post(String url, Map<String, Object> params) {
        return postWithHeader(url, params, null);
    }
    /** 向指定的 url 进行 post 请求(表单) */
    public static ResponseData postWithHeader(String url, Map<String, Object> params, Map<String, Object> headers) {
        // Content-Type 不设置则默认是 application/x-www-form-urlencoded
        return handleRequest("POST", url, U.formatParam(false, params), headers);
    }

    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postBody(String url, String json) {
        return postBodyWithHeader(url, json, null);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postBodyWithHeader(String url, String json, Map<String, Object> headers) {
        Map<String, Object> headerMap = new LinkedHashMap<>();
        if (A.isNotEmpty(headers)) {
            headerMap.putAll(headers);
        }
        headerMap.put("Content-Type", "application/json");
        return handleRequest("POST", url, U.toStr(json), headerMap);
    }


    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static ResponseData put(String url, String json) {
        return putWithHeader(url, json, null);
    }
    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static ResponseData putWithHeader(String url, String json, Map<String, Object> headers) {
        Map<String, Object> headerMap = new LinkedHashMap<>();
        if (A.isNotEmpty(headers)) {
            headerMap.putAll(headers);
        }
        headerMap.put("Content-Type", "application/json");
        return handleRequest("PUT", url, U.toStr(json), headerMap);
    }


    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static ResponseData delete(String url, String json) {
        return deleteWithHeader(url, json, null);
    }
    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static ResponseData deleteWithHeader(String url, String json, Map<String, Object> headers) {
        Map<String, Object> headerMap = new LinkedHashMap<>();
        if (A.isNotEmpty(headers)) {
            headerMap.putAll(headers);
        }
        headerMap.put("Content-Type", "application/json");
        return handleRequest("DELETE", url, U.toStr(json), headerMap);
    }


    /** 向指定 url 上传文件 */
    public static String postFile(String url, Map<String, Object> headers, Map<String, Object> params, Map<String, File> files) {
        long start = System.currentTimeMillis();
        HttpURLConnection con = null;
        Map<String, List<String>> reqHeaders = null;
        StringBuilder sbd = new StringBuilder();
        String result = null;
        Map<String, List<String>> resHeaders = null;
        String resCode = "";
        url = HttpConst.handleEmptyScheme(url);
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(HttpConst.CONNECT_TIME_OUT);
            con.setReadTimeout(HttpConst.READ_TIME_OUT);
            if (A.isNotEmpty(headers)) {
                for (Map.Entry<String, ?> entry : headers.entrySet()) {
                    con.setRequestProperty(entry.getKey(), U.toStr(entry.getValue()));
                }
            }
            con.setRequestProperty("User-Agent", USER_AGENT);
            String traceId = LogUtil.getTraceId();
            if (U.isNotBlank(traceId)) {
                con.setRequestProperty(Const.TRACE, traceId);
            }
            String language = LogUtil.getLanguage();
            if (U.isNotBlank(language)) {
                con.setRequestProperty("Accept-Language", language);
            }
            reqHeaders = con.getRequestProperties();
            boolean hasParam = A.isNotEmpty(params);
            boolean hasFile = A.isNotEmpty(files);
            if (hasParam || hasFile) {
                con.setDoOutput(true);

                String boundary = U.uuid16();
                // 使用 from 表单上传文件时, 需设置 enctype="multipart/form-data" 属性
                con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                try (DataOutputStream data = new DataOutputStream(con.getOutputStream())) {
                    if (hasParam) {
                        sbd.append("param(");
                        for (Map.Entry<String, Object> entry : params.entrySet()) {
                            String key = entry.getKey();
                            String value = U.toStr(entry.getValue());

                            data.writeBytes("--" + boundary + "\r\n");
                            String paramInfo = "Content-Disposition: form-data; name=\"%s\"\r\n\r\n";
                            data.writeBytes(String.format(paramInfo, key));
                            data.writeBytes(value + "\r\n");

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

                            data.writeBytes("--" + boundary + "\r\n");
                            String paramInfo = "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n\r\n";
                            data.writeBytes(String.format(paramInfo, key, file.getName()));
                            data.write(Files.readAllBytes(file.toPath()));
                            data.writeBytes("\r\n");

                            sbd.append("<").append(key).append(" : ").append(file).append(">");
                        }
                        sbd.append(")");
                    }
                    data.writeBytes("--" + boundary + "--\r\n");
                    data.flush();
                }
            }

            con.connect();

            resHeaders = con.getHeaderFields();
            resCode = con.getResponseCode() + " ";
            try (
                    InputStream input = con.getInputStream();
                    ByteArrayOutputStream output = new ByteArrayOutputStream()
            ) {
                U.inputToOutput(input, output);
                result = output.toString(StandardCharsets.UTF_8);
            }
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                String print = String.format("upload file[%s]", sbd);
                LogUtil.ROOT_LOG.info(collectContext(start, "POST", url, print, reqHeaders, resCode, resHeaders, 0, result));
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                String print = String.format("upload file[%s]", sbd);
                LogUtil.ROOT_LOG.error(collectContext(start, "POST", url, print, reqHeaders, resCode, resHeaders, 0, result), e);
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return result;
    }

    private static ResponseData handleRequest(String method, String url, String data, Map<String, Object> headers) {
        long start = System.currentTimeMillis();
        HttpURLConnection con = null;
        Map<String, List<String>> reqHeaders = null;
        Map<String, List<String>> resHeaders = null;
        String resCode = "";
        Integer responseCode = null;
        String result = "";
        String originalUrl = HttpConst.handleEmptyScheme(url);
        int count = 0;
        try {
            String connUrl = originalUrl;
            while (true) {
                con = (HttpURLConnection) new URL(connUrl).openConnection();
                con.setRequestMethod(method);
                con.setConnectTimeout(HttpConst.CONNECT_TIME_OUT);
                con.setReadTimeout(HttpConst.READ_TIME_OUT);
                if (A.isNotEmpty(headers)) {
                    for (Map.Entry<String, ?> entry : headers.entrySet()) {
                        con.setRequestProperty(entry.getKey(), U.toStr(entry.getValue()));
                    }
                }
                con.setRequestProperty("User-Agent", USER_AGENT);
                String traceId = LogUtil.getTraceId();
                if (U.isNotBlank(traceId)) {
                    con.setRequestProperty(Const.TRACE, traceId);
                }
                reqHeaders = con.getRequestProperties();
                if (U.isNotBlank(data)) {
                    // 默认值 false, 当向远程服务器传送数据/写数据时, 需设置为 true
                    con.setDoOutput(true);
                    try (OutputStreamWriter output = new OutputStreamWriter(con.getOutputStream())) {
                        output.write(data);
                        output.flush();
                    }
                }

                con.connect();

                responseCode = con.getResponseCode();
                switch (responseCode) {
                    // 301 和 302 自动进行重定向
                    case HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP -> {
                        connUrl = URLDecoder.decode(con.getHeaderField("Location"), StandardCharsets.UTF_8);
                        count++;
                        continue;
                    }
                }
                break;
            }

            resCode = responseCode + " ";
            try (
                    InputStream input = con.getInputStream();
                    ByteArrayOutputStream output = new ByteArrayOutputStream()
            ) {
                U.inputToOutput(input, output);
                result = output.toString(StandardCharsets.UTF_8);
            }
            resHeaders = con.getHeaderFields();
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(collectContext(start, method, url, data, reqHeaders, resCode, resHeaders, count, result));
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(collectContext(start, method, url, data, reqHeaders, resCode, resHeaders, count, result), e);
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return new ResponseData(responseCode, result);
    }
    private static String collectContext(long start, String method, String url, String params,
                                         Map<String, List<String>> reqHeaders, String resCode,
                                         Map<String, List<String>> resHeaders, int redirectCount, String result) {
        StringBuilder sbd = new StringBuilder();
        long now = System.currentTimeMillis();
        sbd.append("HttpUrlConnection => [")
                .append(DateUtil.formatDateTimeMs(new Date(start))).append(" -> ")
                .append(DateUtil.formatDateTimeMs(new Date(now)))
                .append("(").append(DateUtil.toHuman(now - start)).append(")")
                .append("] (").append(method).append(" ").append(url).append(")");
        if (redirectCount > 0) {
            sbd.append(" redirect-count(").append(redirectCount).append(")");
        }
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
                String key = entry.getKey();
                String value = A.toStr(entry.getValue());
                sbd.append("<").append(key).append(" : ").append(DesensitizationUtil.desByKey(key, value)).append(">");
            }
            sbd.append(")");
        }
        sbd.append("], res[").append(resCode);
        boolean hasResHeader = A.isNotEmpty(resHeaders);
        if (hasResHeader) {
            sbd.append("header(");
            for (Map.Entry<String, List<String>> entry : resHeaders.entrySet()) {
                String key = entry.getKey();
                String value = A.toStr(entry.getValue());
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
