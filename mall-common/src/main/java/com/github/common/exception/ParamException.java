package com.github.common.exception;

import com.github.common.util.A;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/** 自定义参数校验异常 */
@Getter
public class ParamException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final Map<String, String> validationErrorMap;

	public ParamException(String msg) {
		super(msg);
		this.validationErrorMap = Collections.emptyMap();
	}

	public ParamException(String field, String message) {
		this.validationErrorMap = A.maps(field, message);
	}

	public ParamException(Map<String, String> messageMap) {
		this.validationErrorMap = messageMap;
	}

	@Override
	public Throwable fillInStackTrace() {
		return this;
	}
}
