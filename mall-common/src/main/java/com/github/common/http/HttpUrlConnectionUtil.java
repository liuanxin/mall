package com.github.common.http;

import com.github.common.Const;
import com.github.common.date.DateUtil;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
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
    /** 建立连接的超时时间, 单位: 毫秒 */
    private static final int CONNECT_TIME_OUT = 5000;
    /** 数据交互的时间, 单位: 毫秒 */
    private static final int SOCKET_TIME_OUT = 60000;


    /** 向指定 url 进行 get 请求 */
    public static String get(String url) {
        return get(url, null);
    }
    /** 向指定 url 进行 get 请求. 有参数 */
    public static String get(String url, Map<String, Object> params) {
        return getWithHeader(url, params, null);
    }
    /** 向指定 url 进行 get 请求. 有参数和头 */
    public static String getWithHeader(String url, Map<String, Object> params, Map<String, Object> headerMap) {
        return handleRequest("GET", U.appendUrl(url) + U.formatParam(false, params), null, headerMap);
    }


    /** 向指定的 url 进行 post 请求. 有参数 */
    public static String post(String url, Map<String, Object> params) {
        return postWithHeader(url, params, null);
    }
    /** 向指定的 url 进行 post 请求. 有参数和头 */
    public static String postWithHeader(String url, Map<String, Object> params, Map<String, Object> headers) {
        return handleRequest("POST", url, U.formatParam(false, params), headers);
    }


    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static String postBody(String url, Map<String, Object> params) {
        return postBodyWithHeader(url, params, null);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static String postBodyWithHeader(String url, Map<String, Object> params, Map<String, Object> headers) {
        return postBodyWithHeader(url, JsonUtil.toJson(params), headers);
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


    /** 向指定 url 上传文件 */
    public static String postFile(String url, Map<String, Object> params, Map<String, Object> headers, Map<String, File> files) {
        long start = System.currentTimeMillis();
        HttpURLConnection con = null;
        Map<String, List<String>> reqHeaders = null;
        String result = null;
        Map<String, List<String>> resHeaders = null;
        String resCode = "";
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(CONNECT_TIME_OUT);
            con.setReadTimeout(SOCKET_TIME_OUT);
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
                // 默认值 false, 当向远程服务器传送数据/写数据时, 需设置为 true
                con.setDoOutput(true);

                con.setRequestProperty("Content-Type", "multipart/form-data;boundary=*****");
                try (DataOutputStream dataOutput = new DataOutputStream(con.getOutputStream())) {
                    if (hasParam) {
                        for (Map.Entry<String, Object> entry : params.entrySet()) {
                            dataOutput.writeBytes("--*****\r\n");
                            dataOutput.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n");
                            dataOutput.writeBytes(entry.getValue() + "\r\n");
                        }
                    }

                    if (hasFile) {
                        for (Map.Entry<String, File> entry : files.entrySet()) {
                            File file = entry.getValue();
                            dataOutput.writeBytes("--*****\r\n");
                            dataOutput.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey()
                                    + "\";filename=\"" + file.getName() + "\"\r\n\r\n");
                            dataOutput.write(Files.readAllBytes(file.toPath()));
                            dataOutput.writeBytes("\r\n");
                        }
                    }
                    dataOutput.writeBytes("--*****--\r\n");
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
                LogUtil.ROOT_LOG.info(collectContext(start, "POST", url, "upload file", reqHeaders, resCode, resHeaders, result));
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(collectContext(start, "POST", url, "upload file", reqHeaders, resCode, resHeaders, result), e);
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return result;
    }

    private static String handleRequest(String method, String url, String data, Map<String, Object> headerMap) {
        long start = System.currentTimeMillis();
        HttpURLConnection con = null;
        Map<String, List<String>> reqHeaders = null;
        String result = null;
        Map<String, List<String>> resHeaders = null;
        String resCode = "";
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod(method);
            con.setConnectTimeout(CONNECT_TIME_OUT);
            con.setReadTimeout(SOCKET_TIME_OUT);
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
    /** 收集上下文中的数据, 以便记录日志 */
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
                sbd.append("<").append(entry.getKey()).append(": ").append(Joiner.on(",").join(entry.getValue())).append(">");
            }
            sbd.append(")");
        }
        sbd.append("], res[").append(resCode);
        boolean hasResHeader = A.isNotEmpty(resHeaders);
        if (hasResHeader) {
            sbd.append("header(");
            for (Map.Entry<String, List<String>> entry : resHeaders.entrySet()) {
                sbd.append("<").append(entry.getKey()).append(": ").append(Joiner.on(",").join(entry.getValue())).append(">");
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
