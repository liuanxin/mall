package com.github.common.util;

import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.regex.Pattern;

/** 在页面渲染时拼接 url 等的工具类 */
public final class Render {

    private static final Pattern HTTP_PATTERN =  Pattern.compile("(?i)^http(s?):");
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("(?i)^.*\\.(css|js|ico|gif|bmp|png|jpg|jpeg)$");

    /** 去掉 html 和 js 注释, 基于正则表达式完成 */
    public static String compress(String source) {
        return Compressor.html(source);
    }

    /** 基于当前项目的绝对路径, 从 spring mvc 中获取 */
    public static MvcUriComponentsBuilder.MethodArgumentBuilder mapping(String name) {
        return MvcUriComponentsBuilder.fromMappingName(name);
    }

    public static String url(String url) {
        // 前缀改成 // 开头(去掉 http 或 https)
        // domain = domain.replaceFirst("(?i)^http(s?):", "");
        return U.isBlank(url) ? U.EMPTY : HTTP_PATTERN.matcher(url).replaceFirst("");
    }

    /**
     * 如果 path 中包含了 #, 则从 spring mvc 的 controller 里面查找具体的 url. 找到后去掉相关类方法上的空参数.<br>
     *
     * @param path 「IC#index ==> 表示 IndexController.index() IC 是类名的大写, index 是方法」或「具体的资源文件路径」
     */
    public static String urlWithMethod(String path) {
        if (path.contains("#")) {
            // 从 mvc 中获取 url 时的参数类似于 IC#index ==> IC 表示 IndexController 的两个大写字母, index 表示类里的方法
            try {
                path = mapping(path).build();
                // 从 spring mvc 中的 controller 中获取的 path 去掉后面的空参数
                if (path.contains("?")) {
                    path = path.substring(0, path.indexOf("?"));
                }
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                    LogUtil.ROOT_LOG.debug("no path({}) in spring mvc", path);
                }
            }
        }
        if (U.isNotBlank(path)) {
            path = path.startsWith("/") ? path.substring(1) : path;
        }
        return url(U.toStr(path));
    }
}
