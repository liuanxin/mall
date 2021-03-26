package com.github.global.config;

import com.github.common.Const;
import com.github.common.exception.*;
import com.github.common.json.JsonResult;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtils;
import com.github.common.util.U;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 处理全局异常的控制类
 *
 * @see org.springframework.boot.web.servlet.error.ErrorController
 * @see org.springframework.boot.autoconfigure.web.ErrorProperties
 * @see org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
 */
@ConditionalOnClass({ HttpServletRequest.class, ResponseEntity.class })
@RestControllerAdvice
public class GlobalException {

    @Value("${online:false}")
    private boolean online;

    /** 业务异常 */
    @ExceptionHandler(ServiceException.class)
    public JsonResult<Void> service(ServiceException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("业务异常", e);
        }
        return JsonResult.fail(e.getMessage());
    }
    /** 未登录 */
    @ExceptionHandler(NotLoginException.class)
    public JsonResult<Void> notLogin(NotLoginException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("没登录", e);
        }
        return JsonResult.needLogin(e.getMessage());
    }
    /** 无权限 */
    @ExceptionHandler(ForbiddenException.class)
    public JsonResult<Void> forbidden(ForbiddenException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("没权限", e);
        }
        return JsonResult.needPermission(e.getMessage());
    }
    /** 404 */
    @ExceptionHandler(NotFoundException.class)
    public JsonResult<Void> notFound(NotFoundException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("404", e);
        }
        return JsonResult.notFound(e.getMessage());
    }
    /** 错误的请求 */
    @ExceptionHandler(BadRequestException.class)
    public JsonResult<Void> badRequest(BadRequestException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("错误的请求", e);
        }
        return JsonResult.badRequest(e.getMessage());
    }


    // 以下是 spring 的内部异常

    @ExceptionHandler(NoHandlerFoundException.class)
    public JsonResult<Void> noHandler(NoHandlerFoundException e) {
        String msg = online ? "404" : String.format("404(%s %s)", e.getHttpMethod(), e.getRequestURL());

        bindAndPrintLog(msg, e);
        return JsonResult.notFound(msg);
    }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public JsonResult<Void> missParam(MissingServletRequestParameterException e) {
        String msg = online
                ? "缺少必须的参数"
                : String.format("缺少必须的参数(%s), 类型(%s)", e.getParameterName(), e.getParameterType());

        bindAndPrintLog(msg, e);
        return JsonResult.badRequest(msg);
    }
    @ExceptionHandler(MissingRequestHeaderException.class)
    public JsonResult<Void> missHeader(MissingRequestHeaderException e) {
        String msg = online ? "缺少必须的信息" : String.format("缺少头(%s)", e.getHeaderName());

        bindAndPrintLog(msg, e);
        return JsonResult.badRequest(msg);
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public JsonResult<Void> notSupported(HttpRequestMethodNotSupportedException e) {
        String msg = online
                ? "不支持此种方式"
                : String.format("不支持此请求方式: 当前(%s), 支持(%s)", e.getMethod(), A.toStr(e.getSupportedMethods()));

        bindAndPrintLog(msg, e);
        return JsonResult.fail(msg, errorTrack(e));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public JsonResult<Void> uploadSizeExceeded(MaxUploadSizeExceededException e) {
        // 右移 20 位相当于除以两次 1024, 正好表示从字节到 Mb
        String msg = String.format("上传文件太大! 请保持在 %sM 以内", (e.getMaxUploadSize() >> 20));
        bindAndPrintLog(msg, e);
        return JsonResult.fail(msg, errorTrack(e));
    }

    // 以上是 spring 的内部异常


    /** 未知的所有其他异常 */
    @ExceptionHandler(Throwable.class)
    public JsonResult<Void> other(Throwable e) {
        if (LogUtil.ROOT_LOG.isErrorEnabled()) {
            LogUtil.ROOT_LOG.error("有错误", e);
        }

        Throwable cause = e.getCause();
        Throwable t = (cause == null ? e : cause);
        return JsonResult.fail(U.returnMsg(t, online), errorTrack(e));
    }

    // ==================================================

    private void bindAndPrintLog(String msg, Exception e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            // 一些请求不会进入拦截器
            boolean notRequestInfo = LogUtil.hasNotRequestInfo();
            try {
                if (notRequestInfo) {
                    String traceId = RequestUtils.getCookieValue(Const.TRACE);
                    if (U.isBlank(traceId)) {
                        traceId = RequestUtils.getHeaderOrParam(Const.TRACE);
                    }
                    LogUtil.bindContext(traceId, RequestUtils.logContextInfo());
                }
                LogUtil.ROOT_LOG.debug(msg, e);
            } finally {
                if (notRequestInfo) {
                    LogUtil.unbind();
                }
            }
        }
    }
    private List<String> errorTrack(Throwable e) {
        if (online) {
            return null;
        } else {
            List<String> errorList = Lists.newArrayList();
            errorList.add(e.getMessage());
            for (StackTraceElement trace : e.getStackTrace()) {
                errorList.add(trace.toString());
            }
            return errorList;
        }
    }
}
