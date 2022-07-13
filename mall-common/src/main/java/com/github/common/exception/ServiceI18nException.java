package com.github.common.exception;

import java.util.List;

/** 国际化业务异常 */
public class ServiceI18nException extends RuntimeException {

    private final String code;
    private final List<Object> args;

    public ServiceI18nException(String code) {
        this(code, null);
    }
    public ServiceI18nException(String code, List<Object> args) {
        this.code = code;
        this.args = args;
    }

    public String getCode() {
        return code;
    }
    public List<Object> getArgs() {
        return args;
    }
}
