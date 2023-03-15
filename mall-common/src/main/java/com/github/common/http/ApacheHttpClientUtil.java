package com.github.common.http;

import com.github.common.Const;
import com.github.common.date.DateUtil;
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
    /** 每个连接的路由数, 默认是 2. 见: {@link org.apache.http.impl.pool.BasicConnPool} */
    private static final int MAX_CONNECTIONS_PER_ROUTE = 5;
    /** 从连接池获取到连接的超时时间, 单位: 毫秒 */
    private static final int CONNECTION_REQUEST_TIME_OUT = 3000;

    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER;
    private static final HttpRequestRetryHandler HTTP_REQUEST_RETRY_HANDLER;
    static {
        CONNECTION_MANAGER = new PoolingHttpClientConnectionManager();
        CONNECTION_MANAGER.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
        CONNECTION_MANAGER.setMaxTotal(HttpConst.POOL_MAX_TOTAL);

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
                .setConnectionManager(CONNECTION_MANAGER)
                .setConnectionManagerShared(true)
                .setRetryHandler(HTTP_REQUEST_RETRY_HANDLER)
                .build();
    }
    private static RequestConfig config() {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIME_OUT)
                .setConnectTimeout(HttpConst.CONNECT_TIME_OUT)
                .setSocketTimeout(HttpConst.READ_TIME_OUT)
                .build();
    }


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
        HttpGet request = new HttpGet(HttpConst.handleEmptyScheme(HttpConst.appendParamsToUrl(url, params)));
        handleHeader(request, HttpConst.handleContentType(headers, false));
        return handleRequest(request, null);
    }


    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static ResponseData postWithForm(String url, Map<String, Object> params) {
        return postWithForm(url, params, null);
    }
    /** 向指定的 url 进行 post 请求(普通表单方式) */
    public static ResponseData postWithForm(String url, Map<String, Object> params, Map<String, Object> headers) {
        HttpPost request = new HttpPost(HttpConst.handleEmptyScheme(url));
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
        handleHeader(request, HttpConst.handleContentType(headers, false));
        return handleRequest(request, U.formatParam(params));
    }

    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithBody(String url, String data) {
        return postWithBody(url, data, null);
    }
    /** 向指定的 url 基于 post 发起 request-body 请求 */
    public static ResponseData postWithBody(String url, String data, Map<String, Object> headers) {
        HttpPost request = new HttpPost(HttpConst.handleEmptyScheme(url));
        String content = U.toStr(data);
        request.setEntity(new ByteArrayEntity(content.getBytes(StandardCharsets.UTF_8)));
        handleHeader(request, HttpConst.handleContentType(headers, true));
        return handleRequest(request, content);
    }


    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static ResponseData put(String url, String data) {
        return putWithHeader(url, data, null);
    }
    /** 向指定的 url 基于 put 发起 request-body 请求 */
    public static ResponseData putWithHeader(String url, String data, Map<String, Object> headers) {
        HttpPut request = new HttpPut(HttpConst.handleEmptyScheme(url));
        request.setEntity(new ByteArrayEntity(data.getBytes(StandardCharsets.UTF_8)));
        handleHeader(request, HttpConst.handleContentType(headers, true));
        return handleRequest(request, data);
    }


    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static ResponseData delete(String url, String data) {
        return deleteWithHeader(url, data, null);
    }
    /** 向指定的 url 基于 delete 发起 request-body 请求 */
    public static ResponseData deleteWithHeader(String url, String data, Map<String, Object> headers) {
        HttpEntityEnclosingRequestBase request = new HttpEntityEnclosingRequestBase() {
            @Override
            public String getMethod() {
                return "DELETE";
            }
        };
        request.setURI(URI.create(HttpConst.handleEmptyScheme(url)));
        request.setEntity(new ByteArrayEntity(data.getBytes(StandardCharsets.UTF_8)));
        handleHeader(request, HttpConst.handleContentType(headers, true));
        return handleRequest(request, data);
    }


    /** 向指定 url 上传文件(基于 POST + form-data 的方式) */
    public static ResponseData uploadFile(String url, Map<String, Object> headers, Map<String, Object> params, Map<String, File> files) {
        return uploadFile(url, null, headers, params, files);
    }
    /** 向指定 url 上传文件, 只支持 POST|PUT(默认是 POST) + form-data 的方式 */
    public static ResponseData uploadFile(String url, String method, Map<String, Object> headers,
                                          Map<String, Object> params, Map<String, File> files) {
        if (A.isEmpty(params)) {
            params = new HashMap<>();
        }
        StringBuilder sbd = new StringBuilder();

        HttpEntityEnclosingRequestBase request;
        if ("PUT".equalsIgnoreCase(method)) {
            request = new HttpPut(HttpConst.handleEmptyScheme(url));
        } else {
            request = new HttpPost(HttpConst.handleEmptyScheme(url));
        }
        handleHeader(request, HttpConst.handleContentType(headers));
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setLaxMode();
        boolean hasParam = A.isNotEmpty(params);
        if (hasParam) {
            sbd.append("param(");
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = U.toStr(entry.getValue());
                entityBuilder.addTextBody(key, value);
                sbd.append("<").append(key).append(" : ").append(DesensitizationUtil.desByKey(key, value)).append(">");
            }
            sbd.append(")");
        }
        boolean hasFile = A.isNotEmpty(files);
        if (hasFile) {
            if (hasParam) {
                sbd.append(" ");
            }
            sbd.append("file(");
            for (Map.Entry<String, File> entry : files.entrySet()) {
                File file = entry.getValue();
                if (U.isNotNull(file)) {
                    String key = entry.getKey();
                    entityBuilder.addBinaryBody(key, file);
                    sbd.append("<").append(key).append(" : ").append(file.getPath()).append(">");
                }
            }
            sbd.append(")");
        }
        if (hasParam || hasFile) {
            request.setEntity(entityBuilder.build());
        }
        return handleRequest(request, String.format("upload file[%s]", sbd));
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
    private static ResponseData handleRequest(HttpRequestBase request, String params) {
        request.setConfig(config());

        request.setHeader("User-Agent", USER_AGENT);
        String traceId = LogUtil.getTraceId();
        if (U.isNotBlank(traceId)) {
            request.setHeader(Const.TRACE, traceId);
        }
        String language = LogUtil.getLanguage();
        if (U.isNotBlank(language)) {
            request.setHeader("Accept-Language", language);
        }
        String method = request.getMethod();
        String url = request.getURI().toString();

        Header[] reqHeaders = request.getAllHeaders();
        Header[] resHeaders = null;
        Integer responseCode = null;
        String resCode = "";
        String result = null;
        long start = System.currentTimeMillis();
        try (
                CloseableHttpClient httpClient = createHttpClient();
                CloseableHttpResponse response = httpClient.execute(request, HttpClientContext.create())
        ) {
            HttpEntity entity = response.getEntity();
            if (U.isNotNull(entity)) {
                try {
                    resHeaders = response.getAllHeaders();
                    StatusLine status = response.getStatusLine();
                    responseCode = status.getStatusCode();
                    resCode = responseCode + " ";
                    result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                        LogUtil.ROOT_LOG.info(collectContext(start, method, url, params, reqHeaders, resCode, resHeaders, result));
                    }
                } finally {
                    EntityUtils.consume(entity);
                }
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(collectContext(start, method, url, params, reqHeaders, resCode, resHeaders, result), e);
            }
        }
        return new ResponseData(responseCode, result);
    }
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
        boolean hasReqHeader = A.isNotEmpty(reqHeaders);
        if (hasReqHeader) {
            sbd.append("header(");
            for (Header header : reqHeaders) {
                String key = header.getName();
                String value = header.getValue();
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
        sbd.append("], res[").append(statusCode);
        boolean hasResHeader = A.isNotEmpty(resHeaders);
        if (hasResHeader) {
            sbd.append("header(");
            for (Header header : resHeaders) {
                String key = header.getName();
                String value = header.getValue();
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
