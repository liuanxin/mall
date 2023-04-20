package com.github.common.mvc;

import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import org.springframework.context.i18n.LocaleContextHolder;

import javax.servlet.*;
import java.io.IOException;

public class LogTraceFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        try {
            LogUtil.putTraceAndIp(RequestUtil.getTraceId(), RequestUtil.getRealIp(), LocaleContextHolder.getLocale());
            chain.doFilter(req, res);
        } finally {
            LogUtil.unbind();
        }
    }
}
