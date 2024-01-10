package com.github.global.service;

import com.github.common.Const;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;

@Configuration
@RequiredArgsConstructor
public class I18nService {

    private final MessageSource messageSource;

    public String getMessage(String code, List<?> args) {
        return getMessage(code, A.isEmpty(args) ? null : args.toArray(new Object[0]));
    }

    public String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("i18n exception", e);
            }

            Locale defaultLocale = Const.DEFAULT_LOCALE;
            if (!locale.equals(defaultLocale)) {
                try {
                    return messageSource.getMessage(code, args, defaultLocale);
                } catch (Exception ex) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error("default i18n exception", ex);
                    }
                }
            }
            return code;
        }
    }
}
