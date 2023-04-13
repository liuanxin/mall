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
import java.util.*;

@SuppressWarnings("DuplicatedCode")
public class HttpUrlConnectionUtil {

    private static final String USER_AGENT = HttpConst.getUserAgent("http_url_connection");


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
        Map<String, Object> headerMap = HttpConst.handleContentType(headers, false);
        return handleRequest("GET", useUrl, null, headerMap);
    }


    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static ResponseData postWithForm(String url, Map<String, Object> params) {
        return postWithForm(url, params, null);
    }
    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static ResponseData postWithForm(String url, Map<String, Object> params, Map<String, Object> headers) {
        Map<String, Object> headerMap = HttpConst.handleContentType(headers, false);
        return handleRequest("POST", url, U.formatParam(false, true, params), headerMap);
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
        Map<String, Object> headerMap = HttpConst.handleContentType(headers, true);
        return handleRequest("POST", useUrl, content, headerMap);
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
        Map<String, Object> headerMap = HttpConst.handleContentType(headers, true);
        return handleRequest("PUT", useUrl, content, headerMap);
    }


    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static ResponseData delete(String url, String json) {
        return delete(url, json, null);
    }
    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static ResponseData delete(String url, String json, Map<String, Object> headers) {
        Map<String, Object> headerMap = HttpConst.handleContentType(headers, true);
        return handleRequest("DELETE", url, U.toStr(json), headerMap);
    }


    /** 向指定 url 上传文件(基于 POST + form-data 的方式) */
    public static String uploadFile(String url, Map<String, Object> headers, Map<String, Object> params, Map<String, File> files) {
        return uploadFile(url, null, headers, params, files);
    }
    /** 向指定 url 上传文件, 只支持 POST|PUT(默认是 POST) + form-data 的方式 */
    public static String uploadFile(String url, String method, Map<String, Object> headers,
                Map<String, Object> params, Map<String, File> files) {
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
            String useMethod = "PUT".equalsIgnoreCase(method) ? "PUT" : "POST";
            con.setRequestMethod(useMethod);
            con.setConnectTimeout(HttpConst.CONNECT_TIME_OUT);
            con.setReadTimeout(HttpConst.READ_TIME_OUT);
            Map<String, Object> headerMap = HttpConst.handleContentType(headers);
            if (A.isNotEmpty(headerMap)) {
                for (Map.Entry<String, ?> entry : headerMap.entrySet()) {
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
        String useUrl = HttpConst.handleEmptyScheme(url);
        int count = 0;
        try {
            String connectionUrl = useUrl;
            while (true) {
                con = (HttpURLConnection) new URL(connectionUrl).openConnection();
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
                        connectionUrl = URLDecoder.decode(con.getHeaderField("Location"), StandardCharsets.UTF_8);
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
        return new ResponseData(responseCode, handleResponseHeader(resHeaders), result);
    }
    private static Map<String, String> handleResponseHeader(Map<String, List<String>> resHeaders) {
        if (A.isNotEmpty(resHeaders)) {
            Map<String, String> returnMap = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : resHeaders.entrySet()) {
                returnMap.put(entry.getKey(), String.join(",", entry.getValue()));
            }
            return returnMap;
        }
        return Collections.emptyMap();
    }
    private static String headerInfo(Map<String, List<String>> headers) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = String.join(",", entry.getValue());
            list.add("<" + key + " : " + DesensitizationUtil.desByKey(key, value) + ">");
        }
        return String.join("", list);
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
        boolean hasReqHeader = A.isNotEmpty(reqHeaders);
        if (hasReqHeader) {
            sbd.append("header(").append(headerInfo(reqHeaders)).append(")");
        }
        boolean hasParam = U.isNotBlank(params);
        if (hasParam) {
            if (hasReqHeader) {
                sbd.append(" ");
            }
            sbd.append("param|body(").append(U.compress(params)).append(")");
        }
        sbd.append("], res[").append(resCode);
        boolean hasResHeader = A.isNotEmpty(resHeaders);
        if (hasResHeader) {
            sbd.append("header(").append(headerInfo(resHeaders)).append(")");
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
