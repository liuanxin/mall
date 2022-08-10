package com.github.common.mvc;

import com.github.common.Const;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 基于下面的优先级依次获取语言
 * <p>
 * 1. 参数里的 langParamName (默认是 lang)
 * 2. 头里的 langParamName (默认是 lang)
 * 3. 头里的 Accept-Language 值(注意: request.getLocale 是基于 Accept-Language 获取的, 但是只能解析 zh-CN, 当 zh_CN 时将无法解析)
 * 4. 简体中文
 * <p>
 * 手动处理时使用 {@link LocaleContextHolder#getLocale()} 或 {@link RequestContextUtils#getLocale(HttpServletRequest)}
 */
public class LanguageFilter implements Filter {

    private final String languageParam;
    private final Set<Locale> locales;
    public LanguageFilter(String languageParam, String i18nBaseNames) {
        this.languageParam = languageParam;
        this.locales = scanLocale(i18nBaseNames);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) req;
        final Locale locale = handleLocale(httpRequest);

        HttpServletRequestWrapper request = new HttpServletRequestWrapper(httpRequest) {
            @Override
            public Locale getLocale() {
                return locale;
            }
        };

        LocaleContextHolder.setLocale(locale);
        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        if (U.isNotNull(localeResolver)) {
            localeResolver.setLocale(request, (HttpServletResponse) res, locale);
        }
        chain.doFilter(request, res);
    }

    private Set<Locale> scanLocale(String baseNames) {
        if (U.isBlank(baseNames)) {
            return Collections.emptySet();
        }
        String[] files = StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(baseNames));
        if (A.isEmpty(files)) {
            return Collections.emptySet();
        }

        Set<String> languages = new HashSet<>();
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        for (String file : files) {
            try {
                // i18n/messages -> messages, i18n/validation -> validation
                String fileName = file.substring(file.lastIndexOf("/") + 1);
                String location = String.format("classpath*:%s*.properties", file);
                Resource[] resources = new PathMatchingResourcePatternResolver(classLoader).getResources(location);
                for (Resource resource : resources) {
                    // messages.properties, messages_zh_CN.properties, messages_en_US.properties
                    String filename = resource.getFilename();
                    if (U.isNotBlank(filename)) {
                        // noinspection ConstantConditions
                        String name = filename.substring(0, filename.indexOf(".properties"));
                        // 只要 zh_CN、en_US 这些带语言的文件, 不带的忽略
                        if (!name.equals(fileName) && name.startsWith(fileName)) {
                            String language = name.substring(fileName.length() + 1);
                            if (U.isNotBlank(language)) {
                                languages.add(language);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("get({}) resource exception", file, e);
                }
            }
        }
        if (A.isEmpty(languages)) {
            return Collections.emptySet();
        }

        Set<Locale> sets = new HashSet<>();
        for (String lang : languages) {
            Locale locale = parse(lang);
            if (U.isNotNull(locale)) {
                sets.add(locale);
            }
        }
        return sets;
    }

    private Locale handleLocale(HttpServletRequest request) {
        Locale locale = getLocale(request);
        // 「请求的语言」如果是空 或 「请求的语言」不在「国际化对应的语言列表」中 则使用默认语言
        return (hasBlankLocale(locale) || !locales.contains(locale)) ? Const.DEFAULT_LOCALE : locale;
    }

    private Locale getLocale(HttpServletRequest request) {
        String param = U.defaultIfBlank(languageParam, "lang");
        Locale paramLocale = parse(request.getParameter(param));
        if (!hasBlankLocale(paramLocale)) {
            return paramLocale;
        }
        Locale headLocale = parse(request.getHeader(param));
        if (!hasBlankLocale(headLocale)) {
            return headLocale;
        }
        // 从头中获取 Accept-Language, 使用 request.getLocale() 当语言是 en_US 时将无法解析, 只有 en-US 才行
        // return request.getLocale();
        return parse(request.getHeader("Accept-Language"));
    }

    private Locale parse(String lang) {
        if (U.isBlank(lang)) {
            return null;
        }

        // 兼容 zh-CN 和 zh_CN 两种方式
        Locale locale = Locale.forLanguageTag(lang);
        if (U.isNull(locale) || (U.isBlank(locale.getLanguage()) && U.isBlank(locale.getCountry()))) {
            return Locale.forLanguageTag(lang.replace("_", "-"));
        }
        return locale;
    }

    private boolean hasBlankLocale(Locale locale) {
        return U.isNull(locale) || (U.isBlank(locale.getLanguage()) && U.isBlank(locale.getCountry()));
    }
}
