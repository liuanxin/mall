package com.github.common.mvc;

import com.github.common.Const;
import com.github.common.util.A;
import com.github.common.util.U;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** 处理跨域 */
public class CorsFilter implements Filter {

    private static final String ORIGIN = "Origin";
    private static final String CREDENTIALS = "true";
    private static final String METHODS = A.toStr(Const.SUPPORT_METHODS);
    private static final String HEADERS = A.toStr(Const.ALLOW_HEADERS);

    /** @see org.springframework.http.HttpHeaders */
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    // /** for ie: https://www.lovelucy.info/ie-accept-third-party-cookie.html */
    // private static final String P3P = "P3P";
    // private static final String IEP3P = "CP='CAO IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT'";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String origin = request.getHeader(ORIGIN);
        if (U.isNotBlank(origin)) {
            String scheme = request.getScheme();
            int port = request.getServerPort();
            StringBuilder requestDomain = new StringBuilder();
            requestDomain.append(scheme).append("://").append(request.getServerName());
            boolean http = ("http".equals(scheme) && port != 80);
            boolean https = ("https".equals(scheme) && port != 80 && port != 443);
            if (http || https) {
                requestDomain.append(':');
                requestDomain.append(port);
            }
            if (!origin.equals(requestDomain.toString())) {
                response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, CREDENTIALS);
                response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, METHODS);
                response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, HEADERS);
                /*
                if (RequestUtils.isIeRequest() && U.isEmpty(response.getHeader(P3P))) {
                    response.addHeader(P3P, IEP3P);
                }
                */
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
