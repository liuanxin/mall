package com.github.common.exception;

import java.util.Arrays;
import java.util.List;

public class ServiceI18nException extends RuntimeException {

    private final String code;
    private final List<Object> args;

    public ServiceI18nException(String code, Object... args) {
        this.code = code;
        this.args = (args == null) ? null : Arrays.asList(args);
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
