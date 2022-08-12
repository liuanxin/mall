package com.github.common.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.common.Const;
import com.github.common.date.DateUtil;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.DesensitizationUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("DuplicatedCode")
public class ApacheHttpClientUtil {

    private static final String USER_AGENT = HttpConst.getUserAgent("apache_http_client4");

    /** 重试次数 */
    private static final int RETRY_COUNT = 3;
    /** 每个连接的最大连接数, 默认是 20 */
    private static final int MAX_CONNECTIONS = 200;
    /** 每个连接的路由数, 默认是 2 */
    private static final int MAX_CONNECTIONS_PER_ROUTE = 50;
    /** 从连接池获取到连接的超时时间, 单位: 毫秒 */
    private static final int CONNECTION_REQUEST_TIME_OUT = 3000;

    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER;
    private static final HttpRequestRetryHandler HTTP_REQUEST_RETRY_HANDLER;
    static {
        CONNECTION_MANAGER = new PoolingHttpClientConnectionManager();
        CONNECTION_MANAGER.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
        CONNECTION_MANAGER.setMaxTotal(MAX_CONNECTIONS);

        HTTP_REQUEST_RETRY_HANDLER = (exception, executionCount, context) -> {
            if (executionCount > RETRY_COUNT) {
                return false;
            }

            Class<? extends IOException> methodThrowClass = exception.getClass();
            List<Class<? extends IOException>> retryClasses = List.of(
                    NoHttpResponseException.class // 服务器未响应时
            );
            for (Class<? extends IOException> clazz : retryClasses) {
                // parent.isAssignableFrom(child) ==> true, child.isAssignableFrom(parent) ==> false
                if (clazz == methodThrowClass || methodThrowClass.isAssignableFrom(clazz)) {
                    return true;
                }
            }

            List<Class<? extends IOException>> noRetryClasses = List.of(
                    SSLException.class, // SSL 异常
                    InterruptedIOException.class, // 超时
                    UnknownHostException.class, // 目标服务器不可达
                    ConnectException.class // 连接异常
            );
            for (Class<? extends IOException> clazz : noRetryClasses) {
                // parent.isAssignableFrom(child) ==> true, child.isAssignableFrom(parent) ==> false
                if (clazz == methodThrowClass || methodThrowClass.isAssignableFrom(clazz)) {
                    return false;
                }
            }

            HttpRequest request = HttpClientContext.adapt(context).getRequest();
            // 如果请求是幂等的就重试
            return !(request instanceof HttpEntityEnclosingRequest);
        };
    }

    private static CloseableHttpClient createHttpClient() {
        return HttpClients.custom()
                .setUserAgent(USER_AGENT)
                .setConnectionManager(CONNECTION_MANAGER)
                .setRetryHandler(HTTP_REQUEST_RETRY_HANDLER)
                .build();
    }
    private static RequestConfig config(int connectTimeout, int socketTimeout) {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIME_OUT)
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .build();
    }


    /** 向指定 url 进行 get 请求 */
    public static String get(String url) {
        return get(url, HttpConst.CONNECT_TIME_OUT, HttpConst.READ_TIME_OUT);
    }
    public static String get(String url, int connectTimeout, int socketTimeout) {
        url = handleEmptyScheme(url);
        return handleRequest(new HttpGet(url), null, connectTimeout, socketTimeout);
    }

    /** 向指定 url 进行 get 请求 */
    public static <T> String get(String url, T param) {
        return get(url, A.isEmptyObj(param) ? Collections.emptyMap() : JsonUtil.convertType(param, new TypeReference<>() {}));
    }
    /** 向指定 url 进行 get 请求 */
    public static String get(String url, Map<String, Object> params) {
        return get(url, params, HttpConst.CONNECT_TIME_OUT, HttpConst.READ_TIME_OUT);
    }
    /** 向指定 url 进行 get 请求 */
    public static String get(String url, Map<String, Object> params, int connectTimeout, int socketTimeout) {
        url = handleEmptyScheme(url);
        url = handleGetParams(url, params);
        return handleRequest(new HttpGet(url), U.formatParam(params), connectTimeout, socketTimeout);
    }

    /** 向指定 url 进行 get 请求 */
    public static String getWithHeader(String url, Map<String, Object> params, Map<String, Object> headerMap) {
        return getWithHeader(url, params, headerMap, HttpConst.CONNECT_TIME_OUT, HttpConst.READ_TIME_OUT);
    }
    /** 向指定 url 进行 get 请求 */
    public static String getWithHeader(String url, Map<String, Object> params, Map<String, Object> headerMap,
                                       int connectTimeout, int socketTimeout) {
        url = handleEmptyScheme(url);
        url = handleGetParams(url, params);

        HttpGet request = new HttpGet(url);
        handleHeader(request, headerMap);
        return handleRequest(request, U.formatParam(params), connectTimeout, socketTimeout);
    }


    /** 向指定的 url 进行 post 请求 */
    public static String post(String url, Map<String, Object> params) {
        return post(url, params, HttpConst.CONNECT_TIME_OUT, HttpConst.READ_TIME_OUT);
    }
    /** 向指定的 url 进行 post 请求 */
    public static String post(String url, Map<String, Object> params, int connectTimeout, int socketTimeout) {
        url = handleEmptyScheme(url);
        HttpPost request = handlePostParams(url, params);
        return handleRequest(request, U.formatParam(params), connectTimeout, socketTimeout);
    }

    /** 向指定的 url 进行 post 请求 */
    public static String post(String url, String json) {
        return post(url, json, HttpConst.CONNECT_TIME_OUT, HttpConst.READ_TIME_OUT);
    }
    /** 向指定的 url 进行 post 请求 */
    public static String post(String url, String json, int connectTimeout, int socketTimeout) {
        url = handleEmptyScheme(url);
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(json.getBytes(StandardCharsets.UTF_8)));
        request.addHeader("Content-Type", "application/json");
        return handleRequest(request, json, connectTimeout, socketTimeout);
    }

    /** 向指定的 url 进行 post 请求 */
    public static String postWithHeader(String url, Map<String, Object> params, Map<String, Object> headers) {
        return postWithHeader(url, params, headers, HttpConst.CONNECT_TIME_OUT, HttpConst.READ_TIME_OUT);
    }
    /** 向指定的 url 进行 post 请求 */
    public static String postWithHeader(String url, Map<String, Object> params, Map<String, Object> headers,
                                        int connectTimeout, int socketTimeout) {
        url = handleEmptyScheme(url);
        HttpPost request = handlePostParams(url, params);
        handleHeader(request, headers);
        return handleRequest(request, U.formatParam(params), connectTimeout, socketTimeout);
    }

    /** 向指定的 url 进行 post 请求 */
    public static String postBodyWithHeader(String url, String json, Map<String, Object> headers) {
        return postBodyWithHeader(url, json, headers, HttpConst.CONNECT_TIME_OUT, HttpConst.READ_TIME_OUT);
    }
    /** 向指定的 url 进行 post 请求 */
    public static String postBodyWithHeader(String url, String json, Map<String, Object> headers,
                                            int connectTimeout, int socketTimeout) {
        url = handleEmptyScheme(url);
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(json.getBytes(StandardCharsets.UTF_8)));
        handleHeader(request, headers);
        request.addHeader("Content-Type", "application/json");
        return handleRequest(request, json, connectTimeout, socketTimeout);
    }


    /** 向指定的 url 进行 put 请求 */
    public static String put(String url, String json) {
        return putWithHeader(url, json, null);
    }
    /** 向指定的 url 进行 put 请求 */
    public static String putWithHeader(String url, String json, Map<String, Object> headers) {
        return putWithHeader(url, json, headers, HttpConst.CONNECT_TIME_OUT, HttpConst.READ_TIME_OUT);
    }
    /** 向指定的 url 进行 put 请求 */
    public static String putWithHeader(String url, String json, Map<String, Object> headers,
                                       int connectTimeout, int socketTimeout) {
        url = handleEmptyScheme(url);
        HttpPut request = new HttpPut(url);
        request.setEntity(new ByteArrayEntity(json.getBytes(StandardCharsets.UTF_8)));
        handleHeader(request, headers);
        request.addHeader("Content-Type", "application/json");
        return handleRequest(request, json, connectTimeout, socketTimeout);
    }


    /** 向指定的 url 进行 delete 请求 */
    public static String delete(String url, String json) {
        return deleteWithHeader(url, json, null);
    }
    /** 向指定的 url 进行 delete 请求 */
    public static String deleteWithHeader(String url, String json, Map<String, Object> headers) {
        return deleteWithHeader(url, json, headers, HttpConst.CONNECT_TIME_OUT, HttpConst.READ_TIME_OUT);
    }
    /** 向指定的 url 进行 delete 请求 */
    public static String deleteWithHeader(String url, String json, Map<String, Object> headers,
                                          int connectTimeout, int socketTimeout) {
        url = handleEmptyScheme(url);
        HttpEntityEnclosingRequestBase request = new HttpEntityEnclosingRequestBase() {
            @Override
            public String getMethod() {
                return "DELETE";
            }
        };
        request.setURI(URI.create(url));
        request.setEntity(new ByteArrayEntity(json.getBytes(StandardCharsets.UTF_8)));
        handleHeader(request, headers);
        request.addHeader("Content-Type", "application/json");
        return handleRequest(request, json, connectTimeout, socketTimeout);
    }


    /** 向指定 url 上传文件 */
    public static String postFile(String url, Map<String, Object> headers, Map<String, Object> params, Map<String, File> files) {
        url = handleEmptyScheme(url);
        if (A.isEmpty(params)) {
            params = new HashMap<>();
        }
        StringBuilder paramSbd = new StringBuilder();
        HttpPost request = handlePostParams(url, params);
        handleHeader(request, headers);
        boolean hasParam = A.isNotEmpty(params);
        if (hasParam) {
            paramSbd.append("param(");
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = U.toStr(entry.getValue());
                paramSbd.append("<").append(key).append(" : ")
                        .append(DesensitizationUtil.desByKey(key, value)).append(">");
            }
            paramSbd.append(")");
        }
        boolean hasFile = A.isNotEmpty(files);
        if (hasParam && hasFile) {
            paramSbd.append(" ");
        }
        if (hasFile) {
            paramSbd.append("file(");
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setLaxMode();
            for (Map.Entry<String, File> entry : files.entrySet()) {
                File file = entry.getValue();
                if (U.isNotNull(file)) {
                    String key = entry.getKey();
                    entityBuilder.addBinaryBody(key, file);
                    paramSbd.append("<").append(key).append(" : ").append(file).append(">");
                }
            }
            request.setEntity(entityBuilder.build());
            paramSbd.append(")");
        }
        String print = String.format("upload file[%s]", paramSbd);
        return handleRequest(request, print, HttpConst.CONNECT_TIME_OUT, HttpConst.READ_TIME_OUT);
    }


    /** url 如果不是以 http:// 或 https:// 开头就加上 http:// */
    private static String handleEmptyScheme(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        return url;
    }
    /** 处理 get 请求的参数: 拼在 url 上即可 */
    private static String handleGetParams(String url, Map<String, Object> params) {
        if (A.isNotEmpty(params)) {
            url = U.appendUrl(url) + U.formatParam(false, params);
        }
        return url;
    }
    /** 处理 post 请求的参数 */
    private static HttpPost handlePostParams(String url, Map<String, Object> params) {
        HttpPost request = new HttpPost(url);
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        if (A.isNotEmpty(params)) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (U.isNotBlank(key) && U.isNotNull(value)) {
                    nameValuePairs.add(new BasicNameValuePair(key, A.toString(value)));
                }
            }
            request.setEntity(new UrlEncodedFormEntity(nameValuePairs, StandardCharsets.UTF_8));
        }
        return request;
    }
    /** 处理请求时存到 header 中的数据 */
    private static void handleHeader(HttpRequestBase request, Map<String, Object> headers) {
        if (A.isNotEmpty(headers)) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                Object value = entry.getValue();
                if (U.isNotNull(value)) {
                    request.addHeader(entry.getKey(), value.toString());
                }
            }
        }
    }
    /** 发起 http 请求 */
    private static String handleRequest(HttpRequestBase request, String params, int connectTimeout, int socketTimeout) {
        request.setConfig(config(connectTimeout, socketTimeout));

        String traceId = LogUtil.getTraceId();
        if (U.isNotBlank(traceId)) {
            request.setHeader(Const.TRACE, traceId);
        }
        String method = request.getMethod();
        String url = request.getURI().toString();

        Header[] reqHeaders = request.getAllHeaders();
        Header[] resHeaders = null;
        String statusCode = "";
        String result = null;
        long start = System.currentTimeMillis();
        try (
                CloseableHttpClient httpClient = createHttpClient();
                CloseableHttpResponse response = httpClient.execute(request, HttpClientContext.create())
        ) {
            HttpEntity entity = response.getEntity();
            statusCode = response.getStatusLine().getStatusCode() + " ";
            if (U.isNotNull(entity)) {
                resHeaders = response.getAllHeaders();
                result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info(collectContext(start, method, url, params, reqHeaders, statusCode, resHeaders, result));
                }
                EntityUtils.consume(entity);
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(collectContext(start, method, url, params, reqHeaders, statusCode, resHeaders, result), e);
            }
        }
        return result;
    }
    /** 收集上下文中的数据, 以便记录日志 */
    private static String collectContext(long start, String method, String url, String params, Header[] reqHeaders,
                                         String statusCode, Header[] resHeaders, String result) {
        StringBuilder sbd = new StringBuilder();
        long now = System.currentTimeMillis();
        sbd.append("Apache-HttpClient4 => [")
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
            for (Header header : reqHeaders) {
                sbd.append("<").append(header.getName()).append(" : ").append(header.getValue()).append(">");
            }
            sbd.append(")");
        }
        sbd.append("], res[").append(statusCode);
        boolean hasResHeader = A.isNotEmpty(resHeaders);
        if (hasResHeader) {
            sbd.append("header(");
            for (Header header : resHeaders) {
                sbd.append("<").append(header.getName()).append(" : ").append(header.getValue()).append(">");
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
