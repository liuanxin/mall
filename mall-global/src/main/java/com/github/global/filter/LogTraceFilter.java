package com.github.global.filter;

import com.github.common.Const;
import com.github.common.util.DesensitizationUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.common.util.U;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.*;
import java.util.Enumeration;

public class LogTraceFilter implements Filter {

    private final boolean printHeader;
    public LogTraceFilter(boolean printHeader) {
        this.printHeader = printHeader;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest request = (HttpServletRequest) req;
            String traceId = request.getHeader(Const.TRACE);
            String ip = RequestUtil.getRealIp(request);
            LogUtil.putTraceAndIp(traceId, ip, LocaleContextHolder.getLocale());

            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                try (ServletInputStream inputStream = req.getInputStream()) {
                    byte[] bytes = (inputStream == null) ? new byte[0] : inputStream.readAllBytes();
                    printRequestContext(request, ip, bytes);
                    chain.doFilter(new SelfHttpServletRequest(request, bytes), res);
                }
            } else {
                chain.doFilter(req, res);
            }
        } finally {
            LogUtil.unbind();
        }
    }

    private void printRequestContext(HttpServletRequest request, String ip, final byte[] bytes) {
        StringBuilder sbd = new StringBuilder();
        if (printHeader) {
            StringBuilder headerSbd = new StringBuilder();
            Enumeration<String> headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                String headName = headers.nextElement();
                String value = request.getHeader(headName);
                headerSbd.append("<").append(headName).append(" : ");
                headerSbd.append(DesensitizationUtil.desByKey(headName, value));
                headerSbd.append(">");
            }
            if (!headerSbd.toString().isEmpty()) {
                sbd.append(" headers(").append(headerSbd).append(")");
            }
        }
        String params = U.formatParam(true, false, request.getParameterMap());
        if (U.isNotBlank(params)) {
            sbd.append(" params(").append(params).append(")");
        }
        if (bytes.length > 0) {
            sbd.append(" body(").append(new String(bytes)).append(")");
        }

        String method = request.getMethod();
        String url = request.getRequestURL().toString();
        LogUtil.ROOT_LOG.info("[{}] [{} {}] [{}]", ip, method, url, sbd.toString().trim());
    }

    public static class SelfHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] bytes;
        public SelfHttpServletRequest(HttpServletRequest request, byte[] bytes) {
            super(request);
            this.bytes = bytes;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new SelfServletInputStream(bytes);
        }
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
        }
    }

    public static class SelfServletInputStream extends ServletInputStream {

        private final InputStream input;
        public SelfServletInputStream(byte[] bytes) {
            this.input = new ByteArrayInputStream(bytes);
        }

        @Override
        public int read() throws IOException {
            return input.read();
        }
        @Override
        public boolean isFinished() {
            try {
                return input.available() == 0;
            } catch (IOException e) {
                return false;
            }
        }
        @Override
        public boolean isReady() {
            return true;
        }
        @Override
        public void setReadListener(ReadListener readListener) {}
    }
}
