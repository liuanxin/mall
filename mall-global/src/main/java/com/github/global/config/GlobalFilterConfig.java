package com.github.global.config;

import com.github.global.filter.CorsFilter;
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

    /** 支持 cors 的 ip 地址列表 */
    @Value("${http.cors.allow-headers:}")
    private String allowHeaders;

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

    @Bean
    @Order(2)
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsFilter filter = new CorsFilter(allowHeaders);
        FilterRegistrationBean<CorsFilter> filterBean = new FilterRegistrationBean<>(filter);
        filterBean.setOrder(Integer.MIN_VALUE + 2);
        return filterBean;
    }

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
