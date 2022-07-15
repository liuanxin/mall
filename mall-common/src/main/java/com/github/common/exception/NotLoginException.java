package com.github.common.exception;

/** 用户未登录的异常 */
public class NotLoginException extends RuntimeException {

    public NotLoginException() {
        super("请先登录");
    }
    public NotLoginException(String msg) {
        super(msg);
    }
    public NotLoginException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
