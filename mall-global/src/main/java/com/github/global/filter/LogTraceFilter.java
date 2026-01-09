package com.github.global.filter;

import com.github.common.Const;
import com.github.common.util.*;
import org.springframework.context.i18n.LocaleContextHolder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

/**
 * {@link com.github.global.filter.LogTraceFilter} 当前输出请求的 method uri header param<br>
 * {@link com.github.global.config.RequestBodyAdvice} 输出请求中 RequestBody 的 json<br>
 * {@link com.github.global.config.ResponseBodyAdvice} 输出响应中 ResponseBody 的 json
 */
public class LogTraceFilter implements Filter {

    private final List<String> excludePathList;
    private final boolean printHeader;
    public LogTraceFilter(List<String> excludePathList, boolean printHeader) {
        this.excludePathList = excludePathList;
        this.printHeader = printHeader;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(req, res);
            return;
        }
        try {
            String traceId = request.getHeader(Const.TRACE);
            String ip = RequestUtil.getRealIp(request);
            LogUtil.putTraceAndIp(traceId, ip, LocaleContextHolder.getLocale());
            if (A.isEmpty(excludePathList) || !excludePathList.contains(request.getRequestURI())) {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    String method = request.getMethod();
                    String url = RequestUtil.getRequestUrl(request);

                    StringBuilder sbd = new StringBuilder();
                    if (printHeader) {
                        sbd.append("header(");
                        Enumeration<String> headerNames = request.getHeaderNames();
                        while (headerNames.hasMoreElements()) {
                            String headName = headerNames.nextElement();
                            String value = request.getHeader(headName);
                            sbd.append("<").append(headName).append(": ");
                            sbd.append(DesensitizationUtil.desWithKey(headName, value));
                            sbd.append(">");
                        }
                        sbd.append(")");
                    }

                    String params = U.formatPrintParam(request.getParameterMap());
                    if (U.isNotBlank(params)) {
                        sbd.append(" params(").append(params).append(")");
                    }

                    boolean upload = RequestUtil.hasUploadFile(request);
                    if (upload) {
                        sbd.append(" upload-file");
                    }
                    LogUtil.ROOT_LOG.info("[{}] [{} {}] [{}]", ip, method, url, sbd.toString().trim());
                }
            }
            chain.doFilter(req, res);
        } finally {
            LogUtil.unbind();
        }
    }
}
