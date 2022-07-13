package com.github.global.service;

import com.github.common.util.A;
import com.github.common.util.LogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;

@Configuration
@RequiredArgsConstructor
public class I18nService {

    private final MessageSource messageSource;

    public String getMessage(String code) {
        return getMessage(code, (Object) null);
    }

    public String getMessage(String code, List<?> args) {
        Object[] arr = A.isEmpty(args) ? null : args.toArray(new Object[0]);
        return getMessage(code, arr);
    }

    public String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("i18n exception", e);
            }

            if (locale == Locale.SIMPLIFIED_CHINESE) {
                return code;
            }
            try {
                return messageSource.getMessage(code, args, Locale.SIMPLIFIED_CHINESE);
            } catch (NoSuchMessageException ex) {
                return code;
            }
        }
    }
}
