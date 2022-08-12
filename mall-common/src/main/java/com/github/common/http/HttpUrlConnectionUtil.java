package com.github.common.http;

import com.github.common.Const;
import com.github.common.date.DateUtil;
import com.github.common.util.A;
import com.github.common.util.DesensitizationUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.google.common.base.Joiner;

import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("DuplicatedCode")
public class HttpUrlConnectionUtil {

    private static final String USER_AGENT = HttpConst.getUserAgent("url_connection");


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
        return handleRequest("GET", HttpConst.handleGetParams(url, params), null, headerMap);
    }


    /** 向指定的 url 进行 post 请求(表单) */
    public static String post(String url, Map<String, Object> params) {
        return postWithHeader(url, params, null);
    }
    /** 向指定的 url 进行 post 请求(表单) */
    public static String postWithHeader(String url, Map<String, Object> params, Map<String, Object> headers) {
        return handleRequest("POST", url, U.formatParam(false, params), headers);
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
        return handleRequest("POST", url, U.toStr(json), headerMap);
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
        return handleRequest("PUT", url, U.toStr(json), headerMap);
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
        return handleRequest("DELETE", url, U.toStr(json), headerMap);
    }


    /** 向指定 url 上传文件 */
    public static String postFile(String url, Map<String, Object> headers, Map<String, Object> params, Map<String, File> files) {
        long start = System.currentTimeMillis();
        HttpURLConnection con = null;
        Map<String, List<String>> reqHeaders = null;
        StringBuilder paramSbd = new StringBuilder();
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
            reqHeaders = con.getRequestProperties();
            boolean hasParam = A.isNotEmpty(params);
            boolean hasFile = A.isNotEmpty(files);
            if (hasParam || hasFile) {
                con.setDoOutput(true);

                String boundary = "*****";
                con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                try (DataOutputStream dataOutput = new DataOutputStream(con.getOutputStream())) {
                    if (hasParam) {
                        paramSbd.append("param(");
                        for (Map.Entry<String, Object> entry : params.entrySet()) {
                            String key = entry.getKey();
                            String value = U.toStr(entry.getValue());

                            dataOutput.writeBytes("--" + boundary + "\r\n");
                            dataOutput.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n");
                            dataOutput.writeBytes(value + "\r\n");

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

                            dataOutput.writeBytes("--" + boundary + "\r\n");
                            dataOutput.writeBytes("Content-Disposition: form-data; name=\"" + key
                                    + "\";filename=\"" + file.getName() + "\"\r\n\r\n");
                            dataOutput.write(Files.readAllBytes(file.toPath()));
                            dataOutput.writeBytes("\r\n");

                            paramSbd.append("<").append(key).append(" : ").append(file).append(">");
                        }
                        paramSbd.append(")");
                    }
                    dataOutput.writeBytes("--" + boundary + "--\r\n");
                    dataOutput.flush();
                }
            }

            con.connect();

            resHeaders = con.getHeaderFields();
            resCode = con.getResponseCode() + " ";
            try (InputStream input = con.getInputStream()) {
                StringBuilder sbd = new StringBuilder();
                for (int ch; (ch = input.read()) != -1; ) {
                    sbd.append((char) ch);
                }
                result = sbd.toString();
            }
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                String print = String.format("upload file[%s]", paramSbd);
                LogUtil.ROOT_LOG.info(collectContext(start, "POST", url, print, reqHeaders, resCode, resHeaders, result));
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                String print = String.format("upload file[%s]", paramSbd);
                LogUtil.ROOT_LOG.error(collectContext(start, "POST", url, print, reqHeaders, resCode, resHeaders, result), e);
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return result;
    }

    private static String handleRequest(String method, String url, String data, Map<String, Object> headers) {
        long start = System.currentTimeMillis();
        HttpURLConnection con = null;
        Map<String, List<String>> reqHeaders = null;
        String result = null;
        Map<String, List<String>> resHeaders = null;
        String resCode = "";
        url = HttpConst.handleEmptyScheme(url);
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
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

            resHeaders = con.getHeaderFields();
            resCode = con.getResponseCode() + " ";
            try (InputStream input = con.getInputStream()) {
                StringBuilder sbd = new StringBuilder();
                for (int ch; (ch = input.read()) != -1; ) {
                    sbd.append((char) ch);
                }
                result = sbd.toString();
            }
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(collectContext(start, method, url, data, reqHeaders, resCode, resHeaders, result));
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(collectContext(start, method, url, data, reqHeaders, resCode, resHeaders, result), e);
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return result;
    }
    private static String collectContext(long start, String method, String url, String params,
                                         Map<String, List<String>> reqHeaders, String resCode,
                                         Map<String, List<String>> resHeaders, String result) {
        StringBuilder sbd = new StringBuilder();
        long now = System.currentTimeMillis();
        sbd.append("HttpUrlConnection => [")
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
