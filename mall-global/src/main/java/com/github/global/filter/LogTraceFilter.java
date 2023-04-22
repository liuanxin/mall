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

    private static final byte[] EMPTY = new byte[0];

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

            ServletRequest useRequest = req;
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                // 如果用 form + application/x-www-form-urlencoded 的方式请求, getParameterMap 在 getInputStream 后面调用,
                // 会将 form 表单中的数据打印成 body, 且后面的 getParameterMap 为空会导致 web 接口上的参数注入不进去
                // 因此, 保证在 getInputStream 之前先调一下 getParameterMap 就可以避免这个问题
                String params = U.formatParam(true, true, request.getParameterMap());
                boolean upload = RequestUtil.hasUploadFile(request);
                byte[] bytes = EMPTY;
                if (!upload) {
                    // getInputStream 会将 body 中的内容都获取出来(还会将 urlencoded 时的 getParameterMap 值置空)
                    //   其通过内部偏移来读取, 读到末尾后没有指回来, 一些实现流没有实现 reset 方法,
                    //   因此在重复读取的时候将会报 inputStream 默认的实现 mark/reset not supported 异常, 比如
                    //     tomcat 的 org.apache.catalina.connector.CoyoteInputStream
                    //     undertow 的 io.undertow.servlet.spec.ServletInputStreamImpl
                    //     jetty 的 org.eclipse.jetty.server.HttpInputOverHTTP
                    //   这导致当想要重复读取时会报 getXX can't be called after getXXX 异常
                    // 所以像下面这样操作: 先将流读取成 byte[], 再用字节数组构造一个新的输入流返回
                    try (ServletInputStream inputStream = req.getInputStream()) {
                        bytes = U.isNull(inputStream) ? EMPTY : inputStream.readAllBytes();
                        useRequest = new SelfHttpServletRequest(request, bytes);
                    }
                }
                printRequestContext((HttpServletRequest) useRequest, ip, upload, params, bytes);
            }
            chain.doFilter(useRequest, res);
        } finally {
            LogUtil.unbind();
        }
    }

    private void printRequestContext(HttpServletRequest request, String ip, boolean upload, String params, final byte[] bytes) {
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
            if (!headerSbd.isEmpty()) {
                sbd.append(" headers(").append(headerSbd).append(")");
            }
        }
        if (U.isNotBlank(params)) {
            sbd.append(" params(").append(params).append(")");
        }
        if (upload) {
            sbd.append(" upload-file");
        }
        if (bytes.length > 0) {
            sbd.append(" body(").append(U.compress(new String(bytes))).append(")");
        }

        String method = request.getMethod();
        String url = RequestUtil.getRequestUrl(request);
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
            } catch (Exception e) {
                LogUtil.ROOT_LOG.error(e.getMessage());
                return false;
            }
        }
        @Override
        public boolean isReady() {
            return true;
        }
        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }
    }
}
