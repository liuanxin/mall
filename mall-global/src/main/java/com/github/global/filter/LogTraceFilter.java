package com.github.global.filter;

import com.github.common.Const;
import com.github.common.util.*;
import org.springframework.context.i18n.LocaleContextHolder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

public class LogTraceFilter implements Filter {

    private final List<String> excludePathList;
    private final boolean printHeader;
    public LogTraceFilter(List<String> excludePathList, boolean printHeader) {
        this.excludePathList = excludePathList;
        this.printHeader = printHeader;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest request = (HttpServletRequest) req;
            String traceId = request.getHeader(Const.TRACE);
            String ip = RequestUtil.getRealIp(request);
            LogUtil.putTraceAndIp(traceId, ip, LocaleContextHolder.getLocale());
            if (A.isEmpty(excludePathList) || !excludePathList.contains(request.getRequestURI())) {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    String method = request.getMethod();
                    String url = RequestUtil.getRequestUrl(request);
                    String params = U.formatParam(request.getParameterMap());
                    boolean upload = RequestUtil.hasUploadFile(request);
                    printRequest(ip, method, url, headers(request), params, upload);
                }
            }
            chain.doFilter(req, res);
        } finally {
            LogUtil.unbind();
        }
    }

    private String headers(HttpServletRequest request) {
        if (printHeader) {
            StringBuilder sbd = new StringBuilder();
            Enumeration<String> headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                String headName = headers.nextElement();
                String value = request.getHeader(headName);
                sbd.append("<").append(headName).append(" : ");
                sbd.append(DesensitizationUtil.desByKey(headName, value));
                sbd.append(">");
            }
            return sbd.toString();
        } else {
            return null;
        }
    }

    private void printRequest(String ip, String method, String url, String headers, String params, boolean upload) {
        StringBuilder sbd = new StringBuilder();
        if (U.isNotBlank(headers)) {
            sbd.append(" headers(").append(headers).append(")");
        }
        if (U.isNotBlank(params)) {
            sbd.append(" params(").append(params).append(")");
        }
        if (upload) {
            sbd.append(" upload-file");
        }
        LogUtil.ROOT_LOG.info("[{}] [{} {}] [{}]", ip, method, url, sbd.toString().trim());
    }
}
