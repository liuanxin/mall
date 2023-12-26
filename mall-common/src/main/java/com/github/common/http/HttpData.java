package com.github.common.http;

import com.github.common.date.DateUtil;
import com.github.common.util.A;
import com.github.common.util.U;

import java.util.*;

public class HttpData {

    private static final String DEFAULT_METHOD = "GET";
    private static final Set<String> METHOD_SET = new HashSet<>(Arrays.asList(
            DEFAULT_METHOD,
            "POST",
            "PUT",
            "DELETE",
            "PATCH",
            "OPTIONS",
            "HEAD",
            "TRACE"
    ));

    /** @see org.springframework.http.HttpStatus */
    private static final String UNKNOWN_STATUS = "Unknown Status";
    private static final Map<Integer, String> REASONS = new HashMap<>();
    static {
        // informational
        REASONS.put(100, "Continue");
        REASONS.put(101, "Switching Protocol");
        REASONS.put(102, "Processing");
        REASONS.put(103, "Early Hints");

        // successful
        REASONS.put(200, "OK");
        REASONS.put(201, "Created");
        REASONS.put(202, "Accepted");
        REASONS.put(203, "Non-Authoritative Information");
        REASONS.put(204, "No Content");
        REASONS.put(205, "Reset Content");
        REASONS.put(206, "Partial Content");
        REASONS.put(207, "Multi Status");
        REASONS.put(208, "Already Reported");
        REASONS.put(226, "IM Used");

        // redirection
        REASONS.put(300, "Multiple Choice");
        REASONS.put(301, "Moved Permanently");
        REASONS.put(302, "Found");
        REASONS.put(303, "See Other");
        REASONS.put(304, "Not Modified");
        REASONS.put(305, "Use Proxy"); // deprecated
        REASONS.put(307, "Temporary Redirect");
        REASONS.put(308, "Permanent Redirect");

        // client error
        REASONS.put(400, "Bad Request");
        REASONS.put(401, "Unauthorized");
        REASONS.put(402, "Payment Required");
        REASONS.put(403, "Forbidden");
        REASONS.put(404, "Not Found");
        REASONS.put(405, "Method Not Allowed");
        REASONS.put(406, "Not Acceptable");
        REASONS.put(407, "Proxy Authentication Required");
        REASONS.put(408, "Request Timeout");
        REASONS.put(409, "Conflict");
        REASONS.put(410, "Gone");
        REASONS.put(411, "Length Required");
        REASONS.put(412, "Precondition Failed");
        REASONS.put(413, "Payload Too Long");
        REASONS.put(414, "URI Too Long");
        REASONS.put(415, "Unsupported Media Type");
        REASONS.put(416, "Range Not Satisfiable");
        REASONS.put(417, "Expectation Failed");
        REASONS.put(418, "I'm a Teapot");
        REASONS.put(421, "Misdirected Request");
        REASONS.put(422, "Unprocessable Entity");
        REASONS.put(423, "Locked");
        REASONS.put(424, "Failed Dependency");
        REASONS.put(425, "Too Early");
        REASONS.put(426, "Upgrade Required");
        REASONS.put(428, "Precondition Required");
        REASONS.put(429, "Too Many Requests");
        REASONS.put(431, "Request Header Fields Too Large");
        REASONS.put(451, "Unavailable for Legal Reasons");

        // server error
        REASONS.put(500, "Internal Server Error");
        REASONS.put(501, "Not Implemented");
        REASONS.put(502, "Bad Gateway");
        REASONS.put(503, "Service Unavailable");
        REASONS.put(504, "Gateway Timeout");
        REASONS.put(505, "HTTP Version Not Supported");
        REASONS.put(506, "Variant Also Negotiates");
        REASONS.put(507, "Insufficient Storage");
        REASONS.put(508, "Loop Detected");
        REASONS.put(510, "Not Extended");
        REASONS.put(511, "Network Authentication Required");
    }

    private Date reqTime;
    private String method;
    private String url;
    private Map<String, Object> reqHeader;
    private String reqParam;
    private String reqBody;

    private Date resTime;
    private Integer resStatus;
    private Map<String, Object> resHeader;
    private String result;

    private Exception exception;


    public void fillReq(String method, String url, Map<String, Object> reqHeader, String reqParam, String reqBody) {
        this.reqTime = new Date();
        this.method = METHOD_SET.contains(method.toUpperCase()) ? method.toUpperCase() : DEFAULT_METHOD;
        this.url = url;
        this.reqHeader = reqHeader;
        this.reqParam = reqParam;
        this.reqBody = reqBody;
    }

    public void fillRes(Integer status, Map<String, Object> resHeader, String result) {
        this.resTime = new Date();
        this.resStatus = status;
        this.resHeader = resHeader;
        this.result = result;
    }

    public void fillException(Exception exception) {
        this.exception = exception;
    }


    /** see: https://stackoverflow.com/questions/67345954/how-do-i-get-the-http-status-message-from-responses-on-java-net-httpclient-reque */
    public String message() {
        return resStatus != null ? REASONS.getOrDefault(resStatus, UNKNOWN_STATUS) : UNKNOWN_STATUS;
    }

    public Date getReqTime() {
        return reqTime;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, Object> getReqHeader() {
        return reqHeader;
    }

    public String getReqParam() {
        return reqParam;
    }

    public String getReqBody() {
        return reqBody;
    }


    public Date getResTime() {
        return resTime;
    }

    public Integer getResStatus() {
        return resStatus;
    }

    public Map<String, Object> getResHeader() {
        return resHeader;
    }

    public String getResult() {
        return result;
    }

    public Exception getException() {
        return exception;
    }


    @Override
    public String toString() {
        StringBuilder sbd = new StringBuilder();
        if (U.isNotNull(reqTime)) {
            // time
            sbd.append("[").append(DateUtil.formatDateTimeMs(reqTime));
            if (U.isNotNull(resTime)) {
                sbd.append(" -> ").append(DateUtil.formatDateTimeMs(resTime));
            }
            sbd.append("]");

            // method url
            sbd.append(" (").append(U.toStr(method)).append(" ").append(url).append(")");

            // req
            sbd.append(" req[");
            if (A.isNotEmpty(reqHeader)) {
                sbd.append("header(").append(U.printMap(true, reqHeader)).append(")");
            }
            if (U.isNotBlank(reqParam)) {
                if (!sbd.toString().endsWith("[")) {
                    sbd.append(" ");
                }
                sbd.append("param(").append(reqParam).append(")");
            }
            if (U.isNotBlank(reqBody)) {
                if (!sbd.toString().endsWith("[")) {
                    sbd.append(" ");
                }
                sbd.append("body(").append(reqBody).append(")");
            }
            sbd.append("]");
        }

        // res
        if (U.isNotNull(resTime)) {
            sbd.append(" res[");
            sbd.append("(");
            if (U.isNotNull(resStatus)) {
                sbd.append(resStatus).append(" ");
            }
            sbd.append(message()).append(")");
            if (A.isNotEmpty(resHeader)) {
                sbd.append(" header(").append(U.printMap(true, resHeader)).append(")");
            }
            if (U.isNotBlank(result)) {
                sbd.append(" result(").append(result).append(")");
            }
            sbd.append("]");
        }

        // exception
        if (U.isNotNull(exception)) {
            sbd.append(", exception(").append(U.toStr(exception.getMessage()));
            StackTraceElement[] stackTraceArray = exception.getStackTrace();
            if (A.isNotEmpty(stackTraceArray)) {
                for (StackTraceElement trace : stackTraceArray) {
                    sbd.append(", ").append(trace.toString().trim());
                }
            }
            sbd.append(")");
        }
        return sbd.toString();
    }
}
