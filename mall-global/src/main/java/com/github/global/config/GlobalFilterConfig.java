package com.github.global.config;

import com.github.global.filter.LogTraceFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.Filter;
import java.util.List;

@Configuration
@ConditionalOnClass({ Filter.class, FilterRegistrationBean.class })
public class GlobalFilterConfig {

    /** 不输出日志的请求 */
    @Value("${req.log-exclude-path:}")
    private List<String> excludePathList;

    /** 打印请求日志时, 是否输出头信息 */
    @Value("${req.log-print-header:true}")
    private boolean printHeader;

//    /** 处理语言时的参数名(/path?lang=zh-CN) */
//    @Value("${http.language.param-name:lang}")
//    private String languageParam;
//
//    @Value("${spring.messages.basename:}")
//    private String i18nBaseNames;

    /** @see org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration#characterEncodingFilter() */
    @Bean
    @Order(1)
    public FilterRegistrationBean<CharacterEncodingFilter> characterFilter(CharacterEncodingFilter filter) {
        // server.servlet.encoding:
        //   charset: utf-8 # 默认是 UTF_8
        //   force: true
        FilterRegistrationBean<CharacterEncodingFilter> filterBean = new FilterRegistrationBean<>(filter);
        filterBean.setOrder(Integer.MIN_VALUE + 1);
        return filterBean;
    }

//    @Bean
//    @Order(2)
//    public FilterRegistrationBean<CorsFilter> corsFilter() {
//        CorsConfiguration config = new CorsConfiguration();
//
//        // 1. 允许的客户端域名 (对应 Nginx 的 Access-Control-Allow-Origin)
//        // 注意：Spring Boot 2.4 之后，如果 allowCredentials 为 true，这里不能直接用 "*"
//        // 必须明确指定域名，或者使用 allowedOriginPatterns("*") 来支持带有凭证的通配符
//        config.addAllowedOriginPattern("*");
//        // 2. 允许携带凭证 (Cookie/Token)
//        config.setAllowCredentials(true);
//        // 3. 允许的请求头 (对应 Nginx 的 Access-Control-Allow-Headers)
//        config.addAllowedHeader("*"); // Spring 会自动处理，把它展开
//        // 4. 允许的请求方法 (GET, POST, OPTIONS 等)
//        config.addAllowedMethod("*");
//        // 5. 预检请求的缓存时间 (对应 Nginx 的 Access-Control-Max-Age)，单位是秒
//        config.setMaxAge(1728000L);
//        // 6. 为哪些接口配置跨域（这里是全局所有接口）
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//
//        CorsFilter corsFilter = new CorsFilter(source);
//        FilterRegistrationBean<CorsFilter> filterBean = new FilterRegistrationBean<>(corsFilter);
//        filterBean.setOrder(Integer.MIN_VALUE + 2);
//        return filterBean;
//    }

//    @Bean
//    @Order(3)
//    public FilterRegistrationBean<LanguageFilter> languageFilter() {
//        LanguageFilter filter = new LanguageFilter(languageParam, i18nBaseNames);
//        FilterRegistrationBean<LanguageFilter> filterBean = new FilterRegistrationBean<>(filter);
//        filterBean.setOrder(Integer.MIN_VALUE + 3);
//        return filterBean;
//    }

    @Bean
    @Order(4)
    public FilterRegistrationBean<LogTraceFilter> traceFilter() {
//        LogTraceFilter filter = new LogTraceFilter();
        LogTraceFilter filter = new LogTraceFilter(excludePathList, printHeader);
        FilterRegistrationBean<LogTraceFilter> filterBean = new FilterRegistrationBean<>(filter);
        filterBean.setOrder(Integer.MIN_VALUE + 4);
        return filterBean;
    }
}
