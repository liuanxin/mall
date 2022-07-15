package com.github.common.exception;

/** 业务异常 */
public class ServiceException extends RuntimeException {

    public ServiceException(String msg) {
        super(msg);
    }
    public ServiceException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
