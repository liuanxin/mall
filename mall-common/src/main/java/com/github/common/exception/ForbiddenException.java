package com.github.common.exception;

/** 没有访问权限 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String msg) {
        super(msg);
    }
    public ForbiddenException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
