package com.github.common.exception;

import org.springframework.http.ResponseEntity;

/** 业务异常 */
public class ForceReturnException extends RuntimeException {

    private final ResponseEntity<?> response;
    public ForceReturnException(ResponseEntity<?> response) {
        this.response = response;
    }

    public ResponseEntity<?> getResponse() {
        return response;
    }
}
