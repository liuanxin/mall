package com.github.common.util;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <span style="color:red;">
 * !!!
 * 此工具类请只在有 Request 上下文的地方调用(比如 Controller),
 * 在 Service 层调用意味着把 Request 的生命周期放到了更深的业务层.
 * 这不是一个好的习惯, 请不要这么做
 * !!!
 * </span> */
@SuppressWarnings("DuplicatedCode")
public final class RequestUtil {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String USER_AGENT = "User-Agent";
    private static final String REFERRER = "Referer";
    private static final String AJAX_KEY = "X-Requested-With";
    private static final String AJAX_VALUE = "XMLHttpRequest";
    private static final String NGINX_PROTO = "X-Forwarded-Proto";

    private static final String APPLICATION_JSON = "application/json";

    private static final String SCHEME = "//";
    private static final String HTTP = "http:" + SCHEME;
    private static final String HTTPS = "https:" + SCHEME;
    private static final String URL_SPLIT = "/";
    private static final String WWW = "www.";

    /**
     * 获取真实客户端 ip, 关于 X-Forwarded-For 参考: http://zh.wikipedia.org/wiki/X-Forwarded-For<br>
     *
     * 这一 HTTP 头一般格式如: X-Forwarded-For: client1, proxy1, proxy2,<br><br>
     * 其中的值通过一个 逗号 + 空格 把多个 ip 地址区分开,
     * 最左边(client1)是最原始客户端的 ip 地址, 代理服务器每成功收到一个请求, 就把请求来源 ip 地址添加到右边
     */
    public static String getRealIp() {
        HttpServletRequest request = getRequest();
        return U.isNull(request) ? U.EMPTY : getRealIp(request);
    }
    public static String getRealIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Cluster-Client-IP");
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getRemoteAddr();
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        return "";
    }

    /** 获取请求协议, 通常是 http 和 https 两种. https 需要在 nginx 配置中添加 proxy_set_header X-Forwarded-Proto $scheme; 配置 */
    public static String getScheme() {
        return getScheme(getRequest());
    }
    public static String getScheme(HttpServletRequest request) {
        if (U.isNull(request)) {
            return U.EMPTY;
        }

        String proto = request.getHeader(NGINX_PROTO);
        return U.isBlank(proto) ? request.getScheme() : proto.split(",")[0].trim().toLowerCase();
    }

    /** 获取请求的语言信息 */
    public static Locale getLocale() {
        return getLocale(getRequest());
    }
    public static Locale getLocale(HttpServletRequest request) {
        return U.isNull(request) ? LocaleContextHolder.getLocale() : RequestContextUtils.getLocale(request);
    }

    /** 获取 ua 信息 */
    public static String userAgent() {
        return userAgent(getRequest());
    }
    public static String userAgent(HttpServletRequest request) {
        return U.isNull(request) ? U.EMPTY : request.getHeader(USER_AGENT);
    }

    public static String getReferrer() {
        return getReferrer(getRequest());
    }
    public static String getReferrer(HttpServletRequest request) {
        return U.isNull(request) ? U.EMPTY : request.getHeader(REFERRER);
    }

    public static String getMethod() {
        return getMethod(getRequest());
    }
    public static String getMethod(HttpServletRequest request) {
        return U.isNull(request) ? U.EMPTY : request.getMethod();
    }

    /** 获取请求地址, 比如请求的是 http://www.abc.com/x/y 将返回 /x/y */
    public static String getRequestUri() {
        return getRequestUri(getRequest());
    }
    public static String getRequestUri(HttpServletRequest request) {
        return U.isNull(request) ? U.EMPTY : request.getRequestURI();
    }
    /** 获取请求地址, 如 http://www.abc.com/x/y */
    public static String getRequestUrl() {
        return getDomain() + getRequestUri();
    }
    public static String getRequestUrl(HttpServletRequest request) {
        return getDomain(request) + getRequestUri(request);
    }

    /** 返回当前访问的域. 是 request.getRequestURL().toString() 中域的部分, 默认的 scheme 不会返回 https */
    public static String getDomain() {
        return getDomain(getRequest());
    }
    public static String getDomain(HttpServletRequest request) {
        if (U.isNull(request)) {
            return U.EMPTY;
        }

        String scheme = getScheme(request);
        int port = request.getServerPort();
        boolean http = ("http".equals(scheme) && port != 80);
        boolean https = ("https".equals(scheme) && port != 80 && port != 443);

        StringBuilder sbd = new StringBuilder();
        sbd.append(scheme).append("://").append(request.getServerName());
        if (http || https) {
            sbd.append(":").append(port);
        }
        return sbd.toString();
    }

    /** 从 url 中获取 domain 信息. 如: http://www.jd.com/product/123 返回 http://www.jd.com */
    public static String getDomain(String url) {
        if (U.isBlank(url)) {
            return U.EMPTY;
        }

        String lowerUrl = url.toLowerCase();
        if (lowerUrl.startsWith(HTTP)) {
            String tmp = url.substring(HTTP.length());
            return url.substring(0, HTTP.length() + tmp.indexOf(URL_SPLIT));
        } else if (lowerUrl.startsWith(HTTPS)) {
            String tmp = url.substring(HTTPS.length());
            return url.substring(0, HTTPS.length() + tmp.indexOf(URL_SPLIT));
        } else if (lowerUrl.startsWith(SCHEME)) {
            String tmp = url.substring(SCHEME.length());
            return url.substring(0, SCHEME.length() + tmp.indexOf(URL_SPLIT));
        } else {
            return url.substring(0, url.indexOf(URL_SPLIT));
        }
    }

    /** 检查 url 在不在指定的域名中(以根域名检查, 如 www.qq.com 是以 qq.com 为准), 将所在根域名返回, 不在指定域名中则返回空 */
    public static String getDomainInUrl(String url, List<String> domainList) {
        url = getDomain(url);
        if (U.isNotBlank(url) && A.isNotEmpty(domainList)) {
            for (String domain : domainList) {
                String lowerDomain = domain.toLowerCase();
                if (lowerDomain.startsWith(HTTP)) {
                    domain = domain.substring(HTTP.length());
                } else if (lowerDomain.startsWith(HTTPS)) {
                    domain = domain.substring(HTTPS.length());
                } else if (lowerDomain.startsWith(SCHEME)) {
                    domain = domain.substring(SCHEME.length());
                }

                if (domain.toLowerCase().startsWith(WWW)) {
                    domain = domain.substring(WWW.length());
                }
                if (url.toLowerCase().endsWith("." + domain.toLowerCase())) {
                    return domain;
                }
            }
        }
        return U.EMPTY;
    }

    public static Map<String, String> parseParam(boolean des) {
        return parseParam(des, getRequest());
    }
    public static Map<String, String> parseParam(boolean des, HttpServletRequest request) {
        if (U.isNull(request)) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            map.put(entry.getKey(), String.join(",", entry.getValue()));
        }
        return map;
    }

    /**
     * <pre>
     * 格式化参数, 只针对 Content-Type: application/x-www-form-urlencoded 方式.
     *
     * 如果使用文件上传 &lt;from type="multipart/form-data"...&gt; 的方式(Content-Type: multipart/form-data)
     * 或者 RequestBody 的方式(Content-Type: application/json)发送数据,
     * 请求是一个二进制流, 用 request.getParameterMap() 获取到的是一个空数据,
     * 想要获取得基于 request.getInputStream() 或 request.getReader() 或 request.getParts() 操作流,
     * 而获取流后, 当后续要再次获取时将会报 getXX can't be called after getXXX 异常(数据流的偏移指针没有指到最开头),
     * 要解决得包装一层 request 并复制一遍字节码
     * </pre>
     *
     * @param des true 表示脱敏
     * @return 示例: id=xxx&name=yyy
     */
    public static String formatParam(boolean des) {
        return formatParam(des, getRequest());
    }
    public static String formatParam(boolean des, HttpServletRequest request) {
        return U.isNull(request) ? U.EMPTY : U.formatParam(des, true, request.getParameterMap());
    }

    public static boolean hasUploadFile() {
        HttpServletRequest request = getRequest();
        return U.isNotNull(request) && hasUploadFile(request);
    }
    public static boolean hasUploadFile(HttpServletRequest request) {
        return U.toStr(request.getHeader("Content-Type")).toLowerCase().startsWith("multipart/");
    }

    public static Map<String, String> parseHeader(boolean des) {
        return parseHeader(des, getRequest());
    }
    public static Map<String, String> parseHeader(boolean des, HttpServletRequest request) {
        if (U.isNull(request)) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String headName = headers.nextElement();
            String value = request.getHeader(headName);
            map.put(headName, value);
        }
        return map;
    }

    /** 格式化头里的参数: 键值以冒号分隔 */
    public static String formatHeader(boolean des) {
        return formatHeader(des, getRequest());
    }
    public static String formatHeader(boolean des, HttpServletRequest request) {
        if (U.isNull(request)) {
            return U.EMPTY;
        }

        StringBuilder sbd = new StringBuilder();
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String headName = headers.nextElement();
            String value = request.getHeader(headName);
            sbd.append("<").append(headName).append(": ");
            sbd.append(des ? DesensitizationUtil.desWithKey(headName, value) : value);
            sbd.append(">");
        }
        return sbd.toString();
    }

    /** 从请求头中获取值 */
    public static String getHeader(String headerName) {
        HttpServletRequest request = getRequest();
        return U.isNull(request) ? U.EMPTY : request.getHeader(headerName);
    }
    /** 先从请求头中查, 为空再从参数中查 */
    public static String getHeaderOrParam(String param) {
        return getHeaderOrParam(param, getRequest());
    }
    public static String getHeaderOrParam(String param, HttpServletRequest request) {
        if (U.isNull(request)) {
            return U.EMPTY;
        }
        return U.defaultIfBlank(U.defaultIfBlank(request.getHeader(param), request.getParameter(param)), U.EMPTY);
    }

    /** 从 cookie 中获取值 */
    public static String getCookieValue(String name) {
        Cookie cookie = getCookie(name, getRequest());
        return U.isNull(cookie) ? U.EMPTY : cookie.getValue();
    }
    public static String getCookieValue(String name, HttpServletRequest request) {
        Cookie cookie = getCookie(name, request);
        return U.isNull(cookie) ? U.EMPTY : cookie.getValue();
    }
    private static Cookie getCookie(String name, HttpServletRequest request) {
        if (U.isNull(request)) {
            return null;
        }

        Cookie[] cookies = request.getCookies();
        if (A.isNotEmpty(cookies)) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie;
                }
            }
        }
        return null;
    }


    /** 将「json 字符」以 json 格式输出 */
    public static void toJson(String data) {
        render(APPLICATION_JSON, data, getResponse());
    }
    public static void toJson(String data, HttpServletResponse response) {
        render(APPLICATION_JSON, data, response);
    }
    private static void render(String type, String data, HttpServletResponse response) {
        if (U.isNull(response)) {
            return;
        }

        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("return data: " + data);
        }
        try {
            response.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
            response.setContentType(type);
            response.getWriter().write(data);
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("handle json to {} io exception", type, e);
            }
        } catch (Exception e) {
            // 基于 response 调用了 getOutputStream(), 又再调用 getWriter() 会被 web 容器拒绝
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("response exception", e);
            }
        }
    }
    /** 将「json 字符」以 html 格式输出. 不常见! 这种只会在一些特殊的场景用到 */
    public static void toHtml(String data) {
        render("text/html", data, getResponse());
    }
    public static void toHtml(String data, HttpServletResponse response) {
        render("text/html", data, response);
    }


    public static HttpServletRequest getRequest() {
        ServletRequestAttributes requestAttributes = getRequestAttributes();
        return U.isNull(requestAttributes) ? null : requestAttributes.getRequest();
    }

    public static HttpSession getSession() {
        HttpServletRequest request = getRequest();
        return U.isNull(request) ? null : request.getSession();
    }

    public static HttpServletResponse getResponse() {
        ServletRequestAttributes requestAttributes = getRequestAttributes();
        return U.isNull(requestAttributes) ? null : requestAttributes.getResponse();
    }

    private static ServletRequestAttributes getRequestAttributes() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
    }
}
