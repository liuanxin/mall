package com.github.global.filter;

import com.github.common.Const;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.IOException;

public class LogTraceFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest request = (HttpServletRequest) req;
            String traceId = request.getHeader(Const.TRACE);
            String ip = RequestUtil.getRealIp(request);
            LogUtil.putTraceAndIp(traceId, ip, LocaleContextHolder.getLocale());
            chain.doFilter(req, res);
        } finally {
            LogUtil.unbind();
        }
    }

//    private static final byte[] EMPTY = new byte[0];
//
//    private final boolean printHeader;
//    public LogTraceFilter(boolean printHeader) {
//        this.printHeader = printHeader;
//    }
//
//    @Override
//    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
//        try {
//            HttpServletRequest request = (HttpServletRequest) req;
//            String traceId = request.getHeader(Const.TRACE);
//            String ip = RequestUtil.getRealIp(request);
//            LogUtil.putTraceAndIp(traceId, ip, LocaleContextHolder.getLocale());
//            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
//                String method = request.getMethod();
//                String url = RequestUtil.getRequestUrl(request);
//
//                ServletRequest useRequest = request;
//                // 如果用 form + application/x-www-form-urlencoded 的方式请求, getParameterMap 在 getInputStream 后面调用,
//                // 会将 form 表单中的数据打印成 body, 且后面的 getParameterMap 为空会导致 web 接口上的参数注入不进去
//                // 因此, 保证在 getInputStream 之前先调一下 getParameterMap 就可以避免这个问题
//                String params = U.formatParam(request.getParameterMap());
//                boolean upload = RequestUtil.hasUploadFile(request);
//                byte[] bytes = EMPTY;
//                if (!upload) {
//                    // getInputStream 会将 body 中的内容都获取出来(还会将 urlencoded 时的 getParameterMap 值置空)
//                    //   其通过内部偏移来读取, 读到末尾后没有指回来, 一些实现流没有实现 reset 方法,
//                    //   因此在重复读取的时候将会报 inputStream 默认的实现 mark/reset not supported 异常, 比如
//                    //     tomcat 的 org.apache.catalina.connector.CoyoteInputStream
//                    //     undertow 的 io.undertow.servlet.spec.ServletInputStreamImpl
//                    //     jetty 的 org.eclipse.jetty.server.HttpInputOverHTTP
//                    //   这导致当想要重复读取时会报 getXX can't be called after getXXX 异常
//                    // 所以像下面这样操作: 先将流读取成 byte[], 再用字节数组构造一个新的输入流返回
//                    try (ServletInputStream inputStream = request.getInputStream()) {
//                        bytes = U.isNull(inputStream) ? EMPTY : inputStream.readAllBytes();
//                        useRequest = new SelfHttpServletRequest(request, inputStream, bytes);
//                    }
//                }
//                // 处理之前打印请求
//                printRequest(ip, method, url, headers(request), params, upload, bytes);
//                // 处理后续的动作
//                chain.doFilter(useRequest, res);
//            }
//            chain.doFilter(req, res);
//        } finally {
//            LogUtil.unbind();
//        }
//    }
//
//    private String headers(HttpServletRequest request) {
//        if (printHeader) {
//            StringBuilder sbd = new StringBuilder();
//            Enumeration<String> headers = request.getHeaderNames();
//            while (headers.hasMoreElements()) {
//                String headName = headers.nextElement();
//                String value = request.getHeader(headName);
//                sbd.append("<").append(headName).append(" : ");
//                sbd.append(DesensitizationUtil.desByKey(headName, value));
//                sbd.append(">");
//            }
//            return sbd.toString();
//        } else {
//            return null;
//        }
//    }
//
//    private void printRequest(String ip, String method, String url, String headers,
//                              String params, boolean upload, final byte[] bytes) {
//        StringBuilder sbd = new StringBuilder();
//        if (U.isNotBlank(headers)) {
//            sbd.append(" headers(").append(headers).append(")");
//        }
//        if (U.isNotBlank(params)) {
//            sbd.append(" params(").append(params).append(")");
//        }
//        if (upload) {
//            sbd.append(" upload-file");
//        }
//        if (bytes.length > 0) {
//            sbd.append(" body(").append(U.compress(new String(bytes))).append(")");
//        }
//        LogUtil.ROOT_LOG.info("[{}] [{} {}] [{}]", ip, method, url, sbd.toString().trim());
//    }
//
//    public static class SelfHttpServletRequest extends HttpServletRequestWrapper {
//        private final byte[] bytes;
//        private final ServletInputStream is;
//        public SelfHttpServletRequest(HttpServletRequest request, ServletInputStream input, byte[] bytes) {
//            super(request);
//            this.is = input;
//            this.bytes = bytes;
//        }
//
//        @Override
//        public ServletInputStream getInputStream() {
//            return new SelfServletInputStream(bytes, is);
//        }
//        @Override
//        public BufferedReader getReader() throws IOException {
//            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), getCharacterEncoding()));
//        }
//    }
//    public static class SelfServletInputStream extends ServletInputStream {
//        private final InputStream input;
//        private final ServletInputStream is;
//        public SelfServletInputStream(byte[] bytes, ServletInputStream is) {
//            this.input = new ByteArrayInputStream(bytes);
//            this.is = is;
//        }
//
//        @Override
//        public int read() throws IOException {
//            return input.read();
//        }
//        @Override
//        public boolean isFinished() {
//            return is.isFinished();
//        }
//        @Override
//        public boolean isReady() {
//            return is.isReady();
//        }
//        @Override
//        public void setReadListener(ReadListener readListener) {
//            is.setReadListener(readListener);
//        }
//    }
}
