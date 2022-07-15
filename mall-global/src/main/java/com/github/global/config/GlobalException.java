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
import com.github.global.service.I18nService;
import com.github.global.service.ValidationService;
import com.google.common.base.Joiner;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

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
@RequiredArgsConstructor
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

    private final I18nService i18nService;
    private final ValidationService validationService;

    /** 404 */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<JsonResult<String>> notFound(NotFoundException e) {
        int status = returnStatusCode ? JsonCode.NOT_FOUND.getCode() : JsonCode.SUCCESS.getCode();
        return handle("not found", status, JsonResult.notFound(e.getMessage()), e);
    }
    /** 未登录 */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<JsonResult<String>> notLogin(NotLoginException e) {
        int status = returnStatusCode ? JsonCode.NOT_LOGIN.getCode() : JsonCode.SUCCESS.getCode();
        return handle("not login", status, JsonResult.needLogin(e.getMessage()), e);
    }
    /** 无权限 */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<JsonResult<String>> forbidden(ForbiddenException e) {
        int status = returnStatusCode ? JsonCode.NOT_PERMISSION.getCode() : JsonCode.SUCCESS.getCode();
        return handle("forbidden", status, JsonResult.needPermission(e.getMessage()), e);
    }
    /** 参数验证 */
    @ExceptionHandler(ParamException.class)
    public ResponseEntity<JsonResult<String>> param(ParamException e) {
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return handle("param exception", status, JsonResult.badRequest(e.getMessage(), e.getErrorMap()), e);
    }
    /** 错误的请求 */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<JsonResult<String>> badRequest(BadRequestException e) {
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return handle("bad request", status, JsonResult.badRequest(e.getMessage()), e);
    }
    /** 业务异常 */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<JsonResult<String>> service(ServiceException e) {
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        return handle("service exception", status, JsonResult.fail(e.getMessage()), e);
    }
    /** 国际化业务异常 */
    @ExceptionHandler(ServiceI18nException.class)
    public ResponseEntity<JsonResult<String>> serviceI18n(ServiceI18nException e) {
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        String msg = i18nService.getMessage(e.getCode(), e.getArgs());
        return handle("service i18n exception", status, JsonResult.fail(msg), e);
    }
    @ExceptionHandler(ForceReturnException.class)
    public ResponseEntity forceReturn(ForceReturnException e) {
        return e.getResponse();
    }


    // 以下是 spring 的内部异常

    /** 使用 @Valid 注解时, 验证不通过抛出的异常 */
    @ExceptionHandler({ BindException.class, MethodArgumentNotValidException.class })
    public ResponseEntity<JsonResult<String>> paramValidException(BindException e) {
        Map<String, String> errorMap = validationService.validate(e.getBindingResult());
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        String msg = Joiner.on("; ").join(new LinkedHashSet<>(errorMap.values()));
        return handle("valid fail", status, JsonResult.badRequest(msg, errorMap), e);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<JsonResult<String>> noHandler(NoHandlerFoundException e) {
        String msg = online ? "404" : String.format("404(%s %s)", e.getHttpMethod(), e.getRequestURL());
        int status = returnStatusCode ? JsonCode.NOT_FOUND.getCode() : JsonCode.SUCCESS.getCode();
        return handle(msg, status, JsonResult.notFound(msg), e);
    }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<JsonResult<String>> missParam(MissingServletRequestParameterException e) {
        String msg = online ? "miss param" : String.format("miss param(%s : %s)", e.getParameterType(), e.getParameterName());
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return handle(msg, status, JsonResult.badRequest(msg), e);
    }
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<JsonResult<String>> missHeader(MissingRequestHeaderException e) {
        String msg = online ? "miss header" : String.format("miss header(%s)", e.getHeaderName());
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return handle(msg, status, JsonResult.badRequest(msg), e);
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<JsonResult<String>> notSupported(HttpRequestMethodNotSupportedException e) {
        String msg = online ? "not support" : String.format("not support(%s), support(%s)", e.getMethod(), A.toStr(e.getSupportedMethods()));
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        return handle(msg, status, JsonResult.fail(msg), e);
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<JsonResult<String>> uploadSizeExceeded(MaxUploadSizeExceededException e) {
        // 右移 20 位相当于除以两次 1024, 正好表示从字节到 Mb
        String msg = String.format("Upload File size exceeded the limit: %sM", (e.getMaxUploadSize() >> 20));
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        return handle(msg, status, JsonResult.fail(msg), e);
    }

    // 以上是 spring 的内部异常

    /** 未知的所有其他异常 */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity other(Throwable throwable) {
        Throwable e = innerException(1, throwable);
        if (e instanceof NotFoundException nfe) {
            return notFound(nfe);
        } else if (e instanceof NotLoginException nle) {
            return notLogin(nle);
        } else if (e instanceof ForbiddenException fe) {
            return forbidden(fe);
        } else if (e instanceof ParamException pe) {
            return param(pe);
        } else if (e instanceof BadRequestException bre) {
            return badRequest(bre);
        } else if (e instanceof ServiceException se) {
            return service(se);
        } else if (e instanceof ServiceI18nException sie) {
            return serviceI18n(sie);
        } else if (e instanceof ForceReturnException fre) {
            return forceReturn(fre);
        } else {
            int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
            JsonResult<String> result = JsonResult.fail(U.returnMsg(e, online));
            result.setError(collectTrack(e));
            bindAndPrintLog(false, String.format("exception result: (%s)", JsonUtil.toJson(result)), e);
            return ResponseEntity.status(status).body(result);
        }
    }

    private Throwable innerException(int depth, Throwable e) {
        Throwable cause = e.getCause();
        if (cause == null || depth > U.MAX_DEPTH
                || e instanceof NotFoundException
                || e instanceof NotLoginException
                || e instanceof ForbiddenException
                || e instanceof ParamException
                || e instanceof BadRequestException
                || e instanceof ServiceException
                || e instanceof ServiceI18nException
                || e instanceof ForceReturnException) {
            return e;
        }
        return innerException(depth + 1, cause);
    }

    private ResponseEntity<JsonResult<String>> handle(String msg, int status, JsonResult<String> result, Throwable e) {
        result.setError(collectTrack(e));
        bindAndPrintLog(true, String.format("%s, exception result: (%s)", msg, JsonUtil.toJson(result)), e);
        return ResponseEntity.status(status).body(result);
    }

    private void bindAndPrintLog(boolean knowException, String msg, Throwable e) {
        // 检查之前有没有加过日志上下文, 没有就加一下
        boolean logNotTrace = LogUtil.hasNotTraceId();
        try {
            String printMsg;
            if (logNotTrace) {
                String traceId = RequestUtil.getCookieOrHeaderOrParam(Const.TRACE);
                String realIp = RequestUtil.getRealIp();
                LogUtil.putTraceAndIp(traceId, realIp);

                String basicInfo = RequestUtil.logBasicInfo();
                String requestInfo = RequestUtil.logRequestInfo();
                printMsg = String.format("[%s] [%s] %s", basicInfo, requestInfo, msg);
            } else {
                printMsg = msg;
            }

            // 已知异常用 debug, 否则用 error
            if (knowException) {
                if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                    LogUtil.ROOT_LOG.debug("{}", printMsg, e);
                }
            } else {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("{}", printMsg, e);
                }
            }
        } finally {
            if (logNotTrace) {
                LogUtil.unbind();
            }
        }
    }

    private List<String> collectTrack(Throwable e) {
        if (online || U.isNull(e)) {
            return Collections.emptyList();
        }

        List<String> exceptionList = new ArrayList<>();
        exceptionList.add(U.toStr(e.getMessage()));

        StackTraceElement[] stackTraceArray = e.getStackTrace();
        if (A.isNotEmpty(stackTraceArray)) {
            for (StackTraceElement trace : stackTraceArray) {
                exceptionList.add(trace.toString().trim());
            }
        }
        return exceptionList;
    }
}
