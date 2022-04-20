package com.github.common.mvc;

import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

/**
 * 基于下面的优先级依次获取语言
 *
 * 1. 头里的 langParamName
 * 1. 参数里的 langParamName
 * 2. 头里的 Accept-Language
 * 3. request.getLocale()
 * 4. 简体中文
 *
 * 手动处理时使用 {@link LocaleContextHolder#getLocale()} 或 {@link RequestContextUtils#getLocale(HttpServletRequest)}
 */
public class LanguageFilter implements Filter {

    private final String languageParam;
    public LanguageFilter(String languageParam) {
        this.languageParam = languageParam;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;

        String lanParam = U.defaultIfBlank(languageParam, "lang");
        String lan = request.getHeader(lanParam);
        if (U.isBlank(lan)) {
            lan = request.getParameter(lanParam);
        }
        if (U.isBlank(lan)) {
            lan = request.getHeader("Accept-Language");
        }

        Locale locale = null;
        try {
            if (U.isNotBlank(lan)) {
                locale = Locale.forLanguageTag(lan);
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("parse Local exception", e);
            }
        }
        if (U.isNull(locale) || (U.isBlank(locale.getLanguage()) && U.isBlank(locale.getCountry()))) {
            locale = request.getLocale();
        }
        if (U.isNull(locale) || (U.isBlank(locale.getLanguage()) && U.isBlank(locale.getCountry()))) {
            locale = Locale.SIMPLIFIED_CHINESE;
        }

        LocaleContextHolder.setLocale(locale);
        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        if (U.isNotNull(localeResolver)) {
            localeResolver.setLocale(request, (HttpServletResponse) res, locale);
        }

        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
