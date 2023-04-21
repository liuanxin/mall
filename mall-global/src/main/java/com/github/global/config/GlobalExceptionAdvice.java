package com.github.global.config;

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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
@RequiredArgsConstructor
public class GlobalExceptionAdvice {

    @Value("${online:false}")
    private boolean online;

    /**
     * <pre>
     * 响应错误时, 错误码是否以 ResponseStatus 返回
     *
     * true:  ResponseStatus 返回 400 | 500, 返回 json 是 { "data": xxx ... }
     * false: ResponseStatus 返回 200,       返回 json 是 { "code": 400 | 500, "data": xxx ... }
     * </pre>
     */
    @Value("${res.returnStatusCode:false}")
    private boolean returnStatusCode;

    /** 打印请求日志时, 是否输出头信息 */
    @Value("${req.logPrintHeader:true}")
    private boolean printHeader;

    private final I18nService i18nService;
    private final ValidationService validationService;

    private JsonResult<String> handleErrorResult(JsonResult<String> result) {
        if (returnStatusCode) {
            result.setCode(null);
        }
        return result;
    }

    /** 404 */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<JsonResult<String>> notFound(NotFoundException e) {
        int status = (returnStatusCode ? JsonCode.NOT_FOUND : JsonCode.SUCCESS).getCode();
        return handle(true, "not found", status, handleErrorResult(JsonResult.notFound(e.getMessage())), e);
    }
    /** 未登录 */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<JsonResult<String>> notLogin(NotLoginException e) {
        int status = (returnStatusCode ? JsonCode.NOT_LOGIN : JsonCode.SUCCESS).getCode();
        return handle(true, "not login", status, handleErrorResult(JsonResult.needLogin(e.getMessage())), e);
    }
    /** 无权限 */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<JsonResult<String>> forbidden(ForbiddenException e) {
        int status = (returnStatusCode ? JsonCode.NOT_PERMISSION : JsonCode.SUCCESS).getCode();
        return handle(true, "forbidden", status, handleErrorResult(JsonResult.needPermission(e.getMessage())), e);
    }
    /** 参数验证 */
    @ExceptionHandler(ParamException.class)
    public ResponseEntity<JsonResult<String>> param(ParamException e) {
        int status = (returnStatusCode ? JsonCode.BAD_REQUEST : JsonCode.SUCCESS).getCode();
        return handle(true, "param exception", status, handleErrorResult(JsonResult.badRequest(e.getMessage(), e.getErrorMap())), e);
    }
    /** 错误的请求 */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<JsonResult<String>> badRequest(BadRequestException e) {
        int status = (returnStatusCode ? JsonCode.BAD_REQUEST : JsonCode.SUCCESS).getCode();
        return handle(true, "bad request", status, handleErrorResult(JsonResult.badRequest(e.getMessage(), null)), e);
    }
    /** 业务异常 */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<JsonResult<String>> service(ServiceException e) {
        int status = (returnStatusCode ? JsonCode.FAIL : JsonCode.SUCCESS).getCode();
        return handle(true, "service exception", status, handleErrorResult(JsonResult.fail(e.getMessage())), e);
    }
    /** 国际化业务异常 */
    @ExceptionHandler(ServiceI18nException.class)
    public ResponseEntity<JsonResult<String>> serviceI18n(ServiceI18nException e) {
        int status = (returnStatusCode ? JsonCode.FAIL : JsonCode.SUCCESS).getCode();
        String msg = i18nService.getMessage(e.getCode(), e.getArgs());
        return handle(true, "service i18n exception", status, handleErrorResult(JsonResult.fail(msg)), e);
    }
    @ExceptionHandler(ForceReturnException.class)
    public ResponseEntity forceReturn(ForceReturnException e) {
        return e.getResponse();
    }


    // 以下是 spring 的内部异常

    /** 使用 @Validated 注解时, 验证不通过抛出的异常 */
    @ExceptionHandler({ BindException.class, MethodArgumentNotValidException.class })
    public ResponseEntity<JsonResult<String>> paramValidException(BindException e) {
        Map<String, String> errorMap = validationService.validate(e.getBindingResult());
        int status = (returnStatusCode ? JsonCode.BAD_REQUEST : JsonCode.SUCCESS).getCode();
        String msg = String.join("; ", errorMap.values());
        return handle(true, "valid fail", status, handleErrorResult(JsonResult.badRequest(msg, errorMap)), e);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<JsonResult<String>> noHandler(NoHandlerFoundException e) {
        StringBuilder sbd = new StringBuilder("not found");
        if (!online) {
            sbd.append(String.format("(%s -> %s)", e.getHttpMethod(), e.getRequestURL()));
        }
        String msg = sbd.toString();
        int status = (returnStatusCode ? JsonCode.NOT_FOUND : JsonCode.SUCCESS).getCode();
        return handle(true, msg, status, handleErrorResult(JsonResult.notFound(msg)), e);
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<JsonResult<String>> notSupported(HttpRequestMethodNotSupportedException e) {
        StringBuilder sbd = new StringBuilder("not support");
        if (!online) {
            sbd.append(String.format(", current(%s), support(%s)", e.getMethod(), A.toStr(e.getSupportedMethods())));
        }
        String msg = sbd.toString();
        int status = (returnStatusCode ? JsonCode.FAIL : JsonCode.SUCCESS).getCode();
        return handle(true, msg, status, handleErrorResult(JsonResult.fail(msg)), e);
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<JsonResult<String>> uploadSizeExceeded(MaxUploadSizeExceededException e) {
        // 右移 20 位相当于除以两次 1024, 正好表示从字节到 Mb
        String msg = String.format("Upload File size exceeded the limit: %sM", (e.getMaxUploadSize() >> 20));
        int status = (returnStatusCode ? JsonCode.FAIL : JsonCode.SUCCESS).getCode();
        return handle(true, msg, status, handleErrorResult(JsonResult.fail(msg)), e);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<JsonResult<String>> convertJsonException(HttpMessageNotReadableException e) {
        int status = (returnStatusCode ? JsonCode.BAD_REQUEST : JsonCode.SUCCESS).getCode();
        return handle(true, "data convert fail", status, handleErrorResult(JsonResult.badRequest("bad request-body", null)), e);
    }

    // 以上是 spring 的内部异常

    public ResponseEntity<JsonResult<String>> unknown(Throwable e) {
        int status = (returnStatusCode ? JsonCode.FAIL : JsonCode.SUCCESS).getCode();
        return handle(false, null, status, handleErrorResult(JsonResult.fail(returnMsg(online, e))), e);
    }

    /** 未知的所有其他异常 */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity other(Throwable throwable) {
        Throwable exception = innerException(1, throwable);
        if (exception instanceof NotFoundException nfe) {
            return notFound(nfe);
        } else if (exception instanceof NotLoginException nle) {
            return notLogin(nle);
        } else if (exception instanceof ForbiddenException fe) {
            return forbidden(fe);
        } else if (exception instanceof ParamException pe) {
            return param(pe);
        } else if (exception instanceof BadRequestException bre) {
            return badRequest(bre);
        } else if (exception instanceof ServiceException se) {
            return service(se);
        } else if (exception instanceof ServiceI18nException sie) {
            return serviceI18n(sie);
        } else if (exception instanceof ForceReturnException fre) {
            return forceReturn(fre);
        } else {
            return unknown(exception);
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

    private ResponseEntity<JsonResult<String>> handle(boolean knowError, String msg, int status,
                                                      JsonResult<String> result, Throwable e) {
        // 非生产 且 异常信息非空 时, 将异常写入返回值
        if (!online && U.isNotNull(e)) {
            List<String> exceptionList = new ArrayList<>();
            exceptionList.add(U.toStr(e.getMessage()));

            StackTraceElement[] stackTraceArray = e.getStackTrace();
            if (A.isNotEmpty(stackTraceArray)) {
                for (StackTraceElement trace : stackTraceArray) {
                    exceptionList.add(trace.toString().trim());
                }
            }
            result.setError(exceptionList);
        }

        StringBuilder sbd = new StringBuilder();

        String basicInfo = RequestUtil.logBasicInfo();
        String requestInfo = RequestUtil.logRequestInfo(printHeader);
        sbd.append(String.format("[%s] [%s] ", basicInfo, requestInfo));
        if (U.isNotBlank(msg)) {
            sbd.append(msg).append(", ");
        }
        sbd.append(String.format("exception result: (%s)", JsonUtil.toJson(result)));

        if (knowError) {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("{}", sbd, e);
            }
        } else {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("{}", sbd, e);
            }
        }
        return ResponseEntity.status(status)/*.header(Const.TRACE, LogUtil.getTraceId())*/.body(result);
    }

    private static String returnMsg(boolean online, Throwable e) {
        if (online) {
            boolean hasCn = LocaleContextHolder.getLocale() == Locale.CHINA;
            return hasCn ? "出错了, 我们会尽快处理" : "Something went wrong, we'll fix it asap";
        } else {
            return e.getMessage();
        }
    }
}
