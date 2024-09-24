package com.github.common.http;

import com.github.common.date.DateUtil;
import com.github.common.json.JsonUtil;
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
    private String resBody;

    private Exception exception;


    public void fillReq(String method, String url, Map<String, Object> reqHeader, String reqParam, String reqBody) {
        this.reqTime = new Date();
        this.method = METHOD_SET.contains(method.toUpperCase()) ? method.toUpperCase() : DEFAULT_METHOD;
        this.url = url;
        this.reqHeader = reqHeader;
        this.reqParam = reqParam;
        this.reqBody = reqBody;
    }

    public void fillRes(Integer status, Map<String, Object> resHeader, String resBody) {
        this.resTime = new Date();
        this.resStatus = status;
        this.resHeader = resHeader;
        this.resBody = resBody;
    }

    public void fillException(Exception exception) {
        this.exception = exception;
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
    public String getResBody() {
        return resBody;
    }
    public Exception getException() {
        return exception;
    }


    public String reqInfo() {
        List<String> reqList = new ArrayList<>();
        if (A.isNotEmpty(reqHeader)) {
            reqList.add("header(" + U.printMap(true, reqHeader) + ")");
        }
        if (U.isNotBlank(reqParam)) {
            reqList.add("param(" + reqParam + ")");
        }
        if (U.isNotBlank(reqBody)) {
            reqList.add("body(" + dropWhite(reqBody) + ")");
        }
        return String.join(" ", reqList);
    }
    private String dropWhite(String json) {
        if (U.isBlank(json)) {
            return json;
        }
        String t = json.trim();
        if ((t.startsWith("[") && t.endsWith("]")) || (t.startsWith("{") && t.endsWith("}"))) {
            return JsonUtil.toJsonNil(JsonUtil.toObject(t, Object.class));
        } else {
            return json;
        }
    }
    public String resInfo() {
        List<String> resList = new ArrayList<>();
        if (U.isNotNull(resStatus)) {
            resList.add("statusCode(" + resStatus + ")");
        }
        // https://stackoverflow.com/questions/67345954/how-do-i-get-the-http-status-message-from-responses-on-java-net-httpclient-reque
        resList.add("reason(" + REASONS.getOrDefault(resStatus, UNKNOWN_STATUS) + ")");
        if (A.isNotEmpty(resHeader)) {
            resList.add("header(" + U.printMap(true, resHeader) + ")");
        }
        if (U.isNotBlank(resBody)) {
            resList.add("body(" + dropWhite(resBody) + ")");
        }
        return String.join(" ", resList);
    }
    public String exceptionInfo() {
        List<String> exceptionList = new ArrayList<>();
        if (U.isNotNull(exception)) {
            exceptionList.add(exception.getMessage());
            StackTraceElement[] stackTraceArray = exception.getStackTrace();
            if (A.isNotEmpty(stackTraceArray)) {
                for (StackTraceElement trace : stackTraceArray) {
                    exceptionList.add(trace.toString().trim());
                }
            }
        }
        return String.join(",", exceptionList);
    }


    @Override
    public String toString() {
        StringBuilder sbd = new StringBuilder();
        if (U.isNotNull(reqTime)) {
            // time
            sbd.append("[").append(DateUtil.formatDateTimeMs(reqTime));
            if (U.isNotNull(resTime)) {
                sbd.append(" -> ").append(DateUtil.formatDateTimeMs(resTime));
                sbd.append("(").append(DateUtil.toHuman(resTime.getTime() - reqTime.getTime())).append(")");
            }
            sbd.append("]");

            // method url
            sbd.append(" [").append(U.toStr(method)).append(" ").append(url).append("]");

            // req
            String req = reqInfo();
            if (U.isNotBlank(req)) {
                sbd.append(" req[").append(req).append("]");
            }
        }

        // res
        if (U.isNotNull(resTime)) {
            sbd.append(" res[").append(resInfo()).append("]");
        }

        // exception
        if (U.isNotNull(exception)) {
            sbd.append(" exception[").append(exceptionInfo()).append("]");
        }
        return sbd.toString();
    }
}
