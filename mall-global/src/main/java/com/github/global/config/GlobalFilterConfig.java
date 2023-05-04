package com.github.global.config;

import com.github.global.filter.CorsFilter;
import com.github.global.filter.LogTraceFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.CharacterEncodingFilter;

@Configuration
@ConditionalOnClass({ Filter.class, FilterRegistrationBean.class })
public class GlobalFilterConfig {

//    /** 打印请求日志时, 是否输出头信息 */
//    @Value("${req.logPrintHeader:true}")
//    private boolean printHeader;

    /** 支持 cors 的 ip 地址列表 */
    @Value("${http.cors.allow-headers:}")
    private String allowHeaders;

//    /** 处理语言时的参数名(/path?lang=zh-CN) */
//    @Value("${http.language.param-name:lang}")
//    private String languageParam;
//
//    @Value("${spring.messages.basename:}")
//    private String i18nBaseNames;

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
        LogTraceFilter filter = new LogTraceFilter();
//        LogTraceFilter filter = new LogTraceFilter(printHeader);
        FilterRegistrationBean<LogTraceFilter> filterBean = new FilterRegistrationBean<>(filter);
        filterBean.setOrder(Integer.MIN_VALUE + 4);
        return filterBean;
    }
}
