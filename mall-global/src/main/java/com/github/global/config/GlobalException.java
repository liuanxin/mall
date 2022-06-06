package com.github.global.config;

import com.github.common.Const;
import com.github.common.exception.*;
import com.github.common.json.JsonCode;
import com.github.common.json.JsonResult;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.common.util.U;
import com.github.global.util.ValidationUtil;
import com.google.common.base.Joiner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 处理全局异常的控制类
 *
 * @see org.springframework.boot.web.servlet.error.ErrorController
 * @see org.springframework.boot.autoconfigure.web.ErrorProperties
 * @see org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
 */
@SuppressWarnings("rawtypes")
@ConditionalOnClass({ HttpServletRequest.class, ResponseEntity.class })
@RestControllerAdvice
public class GlobalException {

    @Value("${online:false}")
    private boolean online;

    /**
     * 响应错误时, 错误码是否以 ResponseStatus 返回
     *
     * true:  ResponseStatus 返回 400 | 500, 返回 json 是 { "code": 400 | 500 ... }
     * false: ResponseStatus 返回 200,       返回 json 是 { "code": 400 | 500 ... }
     */
    @Value("${res.returnStatusCode:false}")
    private boolean returnStatusCode;

    /** 业务异常 */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<JsonResult> service(ServiceException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("service exception:({})", serviceExceptionTrack(e));
        }
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.fail(e.getMessage()));
    }
    /** 未登录 */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<JsonResult> notLogin(NotLoginException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("not login:({})", serviceExceptionTrack(e));
        }
        int status = returnStatusCode ? JsonCode.NOT_LOGIN.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.needLogin(e.getMessage()));
    }
    /** 无权限 */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<JsonResult> forbidden(ForbiddenException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("forbidden:({})", serviceExceptionTrack(e));
        }
        int status = returnStatusCode ? JsonCode.NOT_PERMISSION.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.needPermission(e.getMessage()));
    }
    /** 404 */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<JsonResult> notFound(NotFoundException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("not found:({})", serviceExceptionTrack(e));
        }
        int status = returnStatusCode ? JsonCode.NOT_FOUND.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.notFound(e.getMessage()));
    }
    /** 参数验证 */
    @ExceptionHandler(ParamException.class)
    public ResponseEntity<JsonResult> param(ParamException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("param exception:({})", serviceExceptionTrack(e));
        }
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.badRequest(e.getMessage(), e.getErrorMap()));
    }
    /** 错误的请求 */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<JsonResult> badRequest(BadRequestException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("bad request:({})", serviceExceptionTrack(e));
        }
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.badRequest(e.getMessage()));
    }
    @ExceptionHandler(ForceReturnException.class)
    public ResponseEntity forceReturn(ForceReturnException e) {
        return e.getResponse();
    }


    // 以下是 spring 的内部异常

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<JsonResult> noHandler(NoHandlerFoundException e) {
        String msg = online ? "404" : String.format("404(%s %s)", e.getHttpMethod(), e.getRequestURL());
        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.NOT_FOUND.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.notFound(msg));
    }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<JsonResult> missParam(MissingServletRequestParameterException e) {
        String msg = online ? "miss param" : String.format("miss param(%s : %s)", e.getParameterType(), e.getParameterName());
        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.badRequest(msg));
    }
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<JsonResult> missHeader(MissingRequestHeaderException e) {
        String msg = online ? "miss header" : String.format("miss header(%s)", e.getHeaderName());
        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.badRequest(msg));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<JsonResult<String>> paramValidException(MethodArgumentNotValidException e) {
        Map<String, String> errorMap = ValidationUtil.validate(e.getBindingResult());
        bindAndPrintLog(JsonUtil.toJson(errorMap), e);
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.badRequest(Joiner.on(",").join(errorMap.values()), errorMap));
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<JsonResult> notSupported(HttpRequestMethodNotSupportedException e) {
        String msg = online ? "not support" : String.format("not support(%s), support(%s)", e.getMethod(), A.toStr(e.getSupportedMethods()));
        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.fail(msg));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<JsonResult> uploadSizeExceeded(MaxUploadSizeExceededException e) {
        // 右移 20 位相当于除以两次 1024, 正好表示从字节到 Mb
        String msg = String.format("Upload File size exceeded the limit: %sM", (e.getMaxUploadSize() >> 20));
        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.fail(msg));
    }

    // 以上是 spring 的内部异常

    private ResponseEntity exception(Throwable e) {
        if (LogUtil.ROOT_LOG.isErrorEnabled()) {
            LogUtil.ROOT_LOG.error("has exception", e);
        }
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        String msg = U.returnMsg(e, online);
        List<String> errorList = (online ? null : collectTrack(e));
        return ResponseEntity.status(status).body(JsonResult.fail(msg, errorList));
    }

    /** 未知的所有其他异常 */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity other(Throwable throwable) {
        Throwable e = innerException(1, throwable);
        if (e instanceof BadRequestException) {
            return badRequest((BadRequestException) e);
        } else if (e instanceof ForbiddenException) {
            return forbidden((ForbiddenException) e);
        } else if (e instanceof ForceReturnException) {
            return forceReturn((ForceReturnException) e);
        } else if (e instanceof NotFoundException) {
            return notFound((NotFoundException) e);
        } else if (e instanceof NotLoginException) {
            return notLogin((NotLoginException) e);
        } else if (e instanceof ParamException) {
            return param((ParamException) e);
        } else if (e instanceof ServiceException) {
            return service((ServiceException) e);
        } else {
            return exception(e);
        }
    }

    // ==================================================

    private Throwable innerException(int depth, Throwable e) {
        Throwable cause = e.getCause();
        if (cause == null || depth > U.MAX_DEPTH
                || e instanceof BadRequestException
                || e instanceof ForbiddenException
                || e instanceof ForceReturnException
                || e instanceof NotFoundException
                || e instanceof NotLoginException
                || e instanceof ParamException
                || e instanceof ServiceException) {
            return e;
        }
        return innerException(depth + 1, cause);
    }

    private void bindAndPrintLog(String msg, Exception e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            // 当没有进到全局拦截器就抛出的异常, 需要这么处理才能在日志中输出整个上下文信息
            boolean logNotStart = LogUtil.hasNotStart();
            try {
                if (logNotStart) {
                    String traceId = RequestUtil.getCookieOrHeaderOrParam(Const.TRACE);
                    String realIp = RequestUtil.getRealIp();
                    LogUtil.putContext(traceId, realIp, RequestUtil.logContextInfo());
                }
                LogUtil.ROOT_LOG.debug(msg, e);
            } finally {
                if (logNotStart) {
                    LogUtil.unbind();
                }
            }
        }
    }
    private List<String> collectTrack(Throwable e) {
        List<String> exceptionList = new ArrayList<>();
        exceptionList.add(e.getMessage().trim());
        for (StackTraceElement trace : e.getStackTrace()) {
            String msg = trace.toString().trim();
            if (msg.startsWith(Const.BASE_PACKAGE)) {
                exceptionList.add(msg);
            } else if (!"...".equals(A.last(exceptionList))) {
                exceptionList.add("...");
            }
        }
        return exceptionList;
    }
    private String serviceExceptionTrack(Throwable e) {
        return Joiner.on(",").join(collectTrack(e));
    }
}
