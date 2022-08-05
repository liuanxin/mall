package com.github.config;

import com.github.common.mvc.SpringMvc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
 * @see org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
 */
@SuppressWarnings("NullableProblems")
@Configuration
public class ManagerWebConfig extends DelegatingWebMvcConfiguration {

    /** 打印请求日志时, 是否输出头信息 */
    @Value("${req.logPrintHeader:true}")
    private boolean printHeader;

    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }

    @SuppressWarnings("deprecation")
    @Override
    protected PathMatchConfigurer getPathMatchConfigurer() {
        // 当使用 @PathVariable("{xx}") 里面有带 . 的参数时, 也能匹配上
        return super.getPathMatchConfigurer().setUseSuffixPatternMatch(false);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        SpringMvc.handlerFormatter(registry);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        StringHttpMessageConverter messageConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        SpringMvc.handlerConvert(converters, StringHttpMessageConverter.class, messageConverter);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        SpringMvc.handlerArgument(argumentResolvers);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ManagerInterceptor(printHeader)).addPathPatterns("/**");
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
