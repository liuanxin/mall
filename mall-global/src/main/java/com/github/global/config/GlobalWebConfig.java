package com.github.global.config;

import com.github.global.converter.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 不要使用继承 WebMvcConfigurationSupport 或 DelegatingWebMvcConfiguration 的方式, 会覆盖掉原有的默认配置
 *
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
 * @see org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
 */
@Configuration
@ConditionalOnClass(WebMvcConfigurer.class)
public class GlobalWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new String2BooleanConverter());
        registry.addConverterFactory(new StringToNumberConverter());
        registry.addConverterFactory(new StringToEnumConverter());
        registry.addConverter(new StringToDateConverter());
        registry.addConverter(new StringToLocalDateConverter());
        registry.addConverter(new StringToLocalDateTimeConverter());
        registry.addConverter(new StringToMoneyConverter());
    }

//    /**
//     * see : http://www.ruanyifeng.com/blog/2016/04/cors.html
//     *
//     * {@link org.springframework.web.servlet.config.annotation.CorsRegistration#CorsRegistration(String)}
//     * {@link GlobalWebConfig#corsFilter}
//     */
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**").allowedMethods(Const.SUPPORT_METHODS);
//    }
}
