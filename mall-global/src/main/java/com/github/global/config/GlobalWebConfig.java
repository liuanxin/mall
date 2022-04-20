package com.github.global.config;

import com.github.common.mvc.CorsFilter;
import com.github.common.mvc.LanguageFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.Filter;

@Configuration
@ConditionalOnClass({ Filter.class })
public class GlobalWebConfig {

    @Value("${http.cors.allow-headers:}")
    private String allowHeaders;

    @Value("${http.language.param-name:lang}")
    private String languageParam;

    @Bean
    @Order(1)
    public FilterRegistrationBean<CharacterEncodingFilter> characterFilter(CharacterEncodingFilter filter) {
        FilterRegistrationBean<CharacterEncodingFilter> filterBean = new FilterRegistrationBean<>(filter);
        filterBean.setOrder(Integer.MIN_VALUE);
        return filterBean;
    }

    @Bean
    @Order(2)
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        FilterRegistrationBean<CorsFilter> filterBean = new FilterRegistrationBean<>(new CorsFilter(allowHeaders));
        filterBean.setOrder(Integer.MIN_VALUE + 1);
        return filterBean;
    }

    @ConditionalOnProperty(prefix = "http", name = "language.handle", value = "true")
    @Bean
    @Order(3)
    public FilterRegistrationBean<LanguageFilter> languageFilter() {
        return new FilterRegistrationBean<>(new LanguageFilter(languageParam));
    }
}
