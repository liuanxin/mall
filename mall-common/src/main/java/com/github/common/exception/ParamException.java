package com.github.common.exception;

import com.github.common.util.Arr;

import java.util.Collections;
import java.util.Map;

/** 自定义参数校验异常 */
public class ParamException extends RuntimeException {

	private final Map<String, String> errorMap;

	public ParamException(String msg) {
		super(msg);
		this.errorMap = Collections.emptyMap();
	}

	public ParamException(Map<String, String> errorMap) {
		super(Arr.isEmpty(errorMap) ? "" : String.join("; ", errorMap.values()));
		this.errorMap = Arr.isEmpty(errorMap) ? Collections.emptyMap() : errorMap;
	}

	public Map<String, String> getErrorMap() {
		return errorMap;
	}
}
