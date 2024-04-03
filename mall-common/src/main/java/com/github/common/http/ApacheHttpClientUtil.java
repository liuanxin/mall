package com.github.common.http;

import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
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
import java.util.concurrent.TimeUnit;

public class ApacheHttpClientUtil {

    // private static final String USER_AGENT = HttpConst.getUserAgent("apache_http_client4");

    /** 重试次数 */
    private static final int RETRY_COUNT = 3;
    /** 每个连接的路由数, 默认是 2. 见: {@link org.apache.http.impl.pool.BasicConnPool} */
    private static final int MAX_CONNECTIONS_PER_ROUTE = 5;
    /** 从连接池获取到连接的超时时间, 单位: 毫秒 */
    private static final int CONNECTION_REQUEST_TIME_OUT = 3000;

    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER;
    private static final HttpRequestRetryHandler HTTP_REQUEST_RETRY_HANDLER;
    private static final DefaultRedirectStrategy REDIRECT_STRATEGY;
    static {
        // 忽略 ssl 证书
        if (TrustCerts.IGNORE_SSL) {
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", new SSLConnectionSocketFactory(TrustCerts.IGNORE_SSL_CONTEXT))
                    .build();
            CONNECTION_MANAGER = new PoolingHttpClientConnectionManager(registry);
        } else {
            CONNECTION_MANAGER = new PoolingHttpClientConnectionManager();
        }
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

        // DefaultRedirectStrategy.INSTANCE 是只对 GET 和 HEAD 重定向
        REDIRECT_STRATEGY = new DefaultRedirectStrategy() {
            @Override
            protected boolean isRedirectable(String method) {
                return true;
            }
        };
    }

    private static CloseableHttpClient createHttpClient() {
        return HttpClients.custom()
                .setConnectionManager(CONNECTION_MANAGER)
                .setConnectionManagerShared(true) // 连接池共享
                .setRetryHandler(HTTP_REQUEST_RETRY_HANDLER)
                .setRedirectStrategy(REDIRECT_STRATEGY)
                .build();
    }
    private static RequestConfig config(int timeoutSecond) {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIME_OUT)
                .setConnectTimeout(HttpConst.CONNECT_TIME_OUT)
                .setSocketTimeout(timeoutSecond > 0 ? ((int) TimeUnit.SECONDS.toMillis(timeoutSecond)) : HttpConst.READ_TIME_OUT)
                // .setRedirectsEnabled(true) // 默认就会处理重定向
                .build();
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
        String useUrl = HttpConst.appendParamsToUrl(HttpConst.handleEmptyScheme(url), params);
        HttpGet request = new HttpGet(useUrl);
        Map<String, Object> headerMap = U.defaultIfNull(headers, new HashMap<>());
        headerMap.put("Content-Type", "application/x-www-form-urlencoded");
        handleHeader(request, headerMap);
        return handleRequest(request, timeoutSecond, null, null, printLog);
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
        Map<String, Object> headerMap = U.defaultIfNull(headers, new HashMap<>());
        headerMap.put("Content-Type", "application/x-www-form-urlencoded");
        handleHeader(request, headerMap);
        return handleRequest(request, timeoutSecond, params, null, printLog);
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
        String useUrl = HttpConst.appendParamsToUrl(HttpConst.handleEmptyScheme(url), params);
        HttpPost request = new HttpPost(useUrl);
        request.setEntity(new ByteArrayEntity(U.toStr(data).getBytes(StandardCharsets.UTF_8)));
        handleHeader(request, headers);
        return handleRequest(request, timeoutSecond, null, data, printLog);
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
    public static HttpData put(String url, String data, Map<String, Object> headers) {
        return put(url, null, data, headers);
    }
    /** 向指定的 url 基于 put 发起请求(json : data 是 json 格式 + header 中的 Content-Type 是 application/json. xml : data 是 xml 格式 + header 中 Content-Type 是 application/xml) */
    public static HttpData put(String url, Map<String, Object> params, String data, Map<String, Object> headers) {
        return put(url, params, data, headers, true);
    }
    /** 向指定的 url 基于 put 发起请求(json : data 是 json 格式 + header 中的 Content-Type 是 application/json. xml : data 是 xml 格式 + header 中 Content-Type 是 application/xml) */
    public static HttpData put(String url, Map<String, Object> params, String data,
                               Map<String, Object> headers, boolean printLog) {
        return put(url, 0, params, data, headers, printLog);
    }
    /** 向指定的 url 基于 put 发起请求(json : data 是 json 格式 + header 中的 Content-Type 是 application/json. xml : data 是 xml 格式 + header 中 Content-Type 是 application/xml) */
    public static HttpData put(String url, int timeoutSecond, Map<String, Object> params, String data,
                               Map<String, Object> headers, boolean printLog) {
        String useUrl = HttpConst.appendParamsToUrl(HttpConst.handleEmptyScheme(url), params);
        HttpPut request = new HttpPut(useUrl);
        request.setEntity(new ByteArrayEntity(U.toStr(data).getBytes(StandardCharsets.UTF_8)));
        handleHeader(request, headers);
        return handleRequest(request, timeoutSecond, null, data, printLog);
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
    public static HttpData delete(String url, int timeoutSecond, String data, Map<String, Object> headers, boolean printLog) {
        HttpEntityEnclosingRequestBase request = new HttpEntityEnclosingRequestBase() {
            @Override
            public String getMethod() {
                return "DELETE";
            }
        };
        request.setURI(URI.create(HttpConst.handleEmptyScheme(url)));
        request.setEntity(new ByteArrayEntity(U.toStr(data).getBytes(StandardCharsets.UTF_8)));
        handleHeader(request, headers);
        return handleRequest(request, timeoutSecond, null, data, printLog);
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
        if (A.isEmpty(params)) {
            params = new HashMap<>();
        }

        HttpEntityEnclosingRequestBase request;
        if ("PUT".equalsIgnoreCase(method)) {
            request = new HttpPut(HttpConst.handleEmptyScheme(url));
        } else {
            request = new HttpPost(HttpConst.handleEmptyScheme(url));
        }
        handleHeader(request, headers);
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setLaxMode();
        boolean hasParam = A.isNotEmpty(params);
        if (hasParam) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = U.toStr(entry.getValue());
                entityBuilder.addTextBody(key, value);
            }
        }
        Map<String, String> fileMap = new LinkedHashMap<>();
        boolean hasFile = A.isNotEmpty(files);
        if (hasFile) {
            for (Map.Entry<String, File> entry : files.entrySet()) {
                File file = entry.getValue();
                if (U.isNotNull(file)) {
                    String key = entry.getKey();
                    entityBuilder.addBinaryBody(key, file);
                    fileMap.put(key, file.toString());
                }
            }
        }
        if (hasParam || hasFile) {
            request.setEntity(entityBuilder.build());
        }
        return handleRequest(request, timeoutSecond, params, JsonUtil.toJsonNil(fileMap), printLog);
    }


    /** 处理请求时存到 header 中的数据 */
    private static void handleHeader(HttpRequestBase request, Map<String, Object> headers) {
        Map<String, Object> headerMap = HttpConst.handleCommonHeader(headers/*, USER_AGENT*/);
        if (A.isNotEmpty(headerMap)) {
            for (Map.Entry<String, Object> entry : headerMap.entrySet()) {
                Object value = entry.getValue();
                if (U.isNotNull(value)) {
                    request.addHeader(entry.getKey(), value.toString());
                }
            }
        }
    }
    private static Map<String, Object> handleHeader(Header[] headers) {
        if (A.isNotEmpty(headers)) {
            Map<String, Object> returnMap = new LinkedHashMap<>();
            for (Header header : headers) {
                returnMap.put(header.getName(), header.getValue());
            }
            return returnMap;
        }
        return Collections.emptyMap();
    }
    private static HttpData handleRequest(HttpRequestBase request, int timeoutSecond, Map<String, Object> params,
                                          String body, boolean printLog) {
        request.setConfig(config(timeoutSecond));
        String method = request.getMethod();
        String url = request.getURI().toString();

        HttpData httpData = new HttpData();
        httpData.fillReq(method, url, handleHeader(request.getAllHeaders()), U.formatPrintParam(params), body);
        try (
                CloseableHttpClient httpClient = createHttpClient();
                CloseableHttpResponse response = httpClient.execute(request, HttpClientContext.create())
        ) {
            HttpEntity entity = response.getEntity();
            if (U.isNotNull(entity)) {
                try {
                    int statusCode = response.getStatusLine().getStatusCode();
                    Map<String, Object> resHeader = handleHeader(response.getAllHeaders());
                    String result = EntityUtils.toString(entity);
                    httpData.fillRes(statusCode, resHeader, result);
                    if (printLog && LogUtil.ROOT_LOG.isInfoEnabled()) {
                        LogUtil.ROOT_LOG.info(httpData.toString());
                    }
                } finally {
                    EntityUtils.consume(entity);
                }
            }
        } catch (Exception e) {
            httpData.fillException(e);
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(httpData.toString(), e);
            }
        }
        return httpData;
    }
}
