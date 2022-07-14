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
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("not found:({})", serviceExceptionTrack(e));
        }
        int status = returnStatusCode ? JsonCode.NOT_FOUND.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(printResult(JsonResult.notFound(e.getMessage())));
    }
    /** 未登录 */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<JsonResult<String>> notLogin(NotLoginException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("not login:({})", serviceExceptionTrack(e));
        }
        int status = returnStatusCode ? JsonCode.NOT_LOGIN.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(printResult(JsonResult.needLogin(e.getMessage())));
    }
    /** 无权限 */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<JsonResult<String>> forbidden(ForbiddenException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("forbidden:({})", serviceExceptionTrack(e));
        }
        int status = returnStatusCode ? JsonCode.NOT_PERMISSION.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(printResult(JsonResult.needPermission(e.getMessage())));
    }
    /** 参数验证 */
    @ExceptionHandler(ParamException.class)
    public ResponseEntity<JsonResult<String>> param(ParamException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("param exception:({})", serviceExceptionTrack(e));
        }
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(printResult(JsonResult.badRequest(e.getMessage(), e.getErrorMap())));
    }
    /** 错误的请求 */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<JsonResult<String>> badRequest(BadRequestException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("bad request:({})", serviceExceptionTrack(e));
        }
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(printResult(JsonResult.badRequest(e.getMessage())));
    }
    /** 业务异常 */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<JsonResult<String>> service(ServiceException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("service exception:({})", serviceExceptionTrack(e));
        }
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(printResult(JsonResult.fail(e.getMessage())));
    }
    /** 国际化业务异常 */
    @ExceptionHandler(ServiceI18nException.class)
    public ResponseEntity<JsonResult<String>> serviceI18n(ServiceI18nException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("service i18n exception:({})", serviceExceptionTrack(e));
        }
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        String msg = i18nService.getMessage(e.getCode(), e.getArgs());
        return ResponseEntity.status(status).body(printResult(JsonResult.fail(msg)));
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
        bindAndPrintLog(JsonUtil.toJson(errorMap), e);
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        String msg = Joiner.on("; ").join(new LinkedHashSet<>(errorMap.values()));
        return ResponseEntity.status(status).body(printResult(JsonResult.badRequest(msg, errorMap)));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<JsonResult<String>> noHandler(NoHandlerFoundException e) {
        String msg = online ? "404" : String.format("404(%s %s)", e.getHttpMethod(), e.getRequestURL());
        // bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.NOT_FOUND.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(printResult(JsonResult.notFound(msg)));
    }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<JsonResult<String>> missParam(MissingServletRequestParameterException e) {
        String msg = online ? "miss param" : String.format("miss param(%s : %s)", e.getParameterType(), e.getParameterName());
        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(printResult(JsonResult.badRequest(msg)));
    }
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<JsonResult<String>> missHeader(MissingRequestHeaderException e) {
        String msg = online ? "miss header" : String.format("miss header(%s)", e.getHeaderName());
        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(printResult(JsonResult.badRequest(msg)));
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<JsonResult<String>> notSupported(HttpRequestMethodNotSupportedException e) {
        String msg = online ? "not support" : String.format("not support(%s), support(%s)", e.getMethod(), A.toStr(e.getSupportedMethods()));
        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(printResult(JsonResult.fail(msg)));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<JsonResult<String>> uploadSizeExceeded(MaxUploadSizeExceededException e) {
        // 右移 20 位相当于除以两次 1024, 正好表示从字节到 Mb
        String msg = String.format("Upload File size exceeded the limit: %sM", (e.getMaxUploadSize() >> 20));
        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(printResult(JsonResult.fail(msg)));
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
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(e.getMessage(), e);
            }
            int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
            String msg = U.returnMsg(e, online);
            return ResponseEntity.status(status).body(printResult(JsonResult.fail(msg, collectTrack(e))));
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

    private JsonResult<String> printResult(JsonResult<String> result) {
        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            LogUtil.ROOT_LOG.info("exception result: ({})", JsonUtil.toJson(result));
        }
        return result;
    }

    private String serviceExceptionTrack(Throwable e) {
        List<String> errorList = collectTrack(e);
        return A.isEmpty(errorList) ? U.EMPTY : Joiner.on(",").join(errorList);
    }

    private List<String> collectTrack(Throwable e) {
        if (online) {
            return Collections.emptyList();
        }
        List<String> exceptionList = new ArrayList<>();
        if (U.isNotNull(e)) {
            exceptionList.add(U.toStr(e.getMessage()));

            StackTraceElement[] stackTraceArray = e.getStackTrace();
            if (A.isNotEmpty(stackTraceArray)) {
                for (StackTraceElement trace : stackTraceArray) {
                    if (U.isNotNull(trace)) {
                        exceptionList.add(trace.toString().trim());
                    }
                }
            }
        }
        return exceptionList;
    }
}
