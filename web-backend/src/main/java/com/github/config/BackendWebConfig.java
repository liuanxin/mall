package com.github.config;

import com.github.common.mvc.SpringMvc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 不要使用继承 WebMvcConfigurationSupport 或 DelegatingWebMvcConfiguration 的方式, 会覆盖掉原有的默认配置
 *
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
 * @see org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
 */
@SuppressWarnings("NullableProblems")
@Configuration
public class BackendWebConfig implements WebMvcConfigurer {

    /** 打印请求日志时, 是否输出头信息 */
    @Value("${req.logPrintHeader:true}")
    private boolean printHeader;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        SpringMvc.handlerFormatter(registry);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        SpringMvc.handlerArgument(argumentResolvers);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new BackendInterceptor(printHeader)).addPathPatterns("/**");
    }
}
