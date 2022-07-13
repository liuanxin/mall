package com.github.common.exception;

import java.io.Serial;
import java.io.Serializable;

/** 用户未登录的异常 */
public class NotLoginException extends RuntimeException implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
