package com.github.common.exception;

import com.github.common.util.A;
import com.google.common.base.Joiner;
import lombok.Getter;

import java.io.Serial;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;

/** 自定义参数校验异常 */
@Getter
public class ParamException extends RuntimeException {
	@Serial
	private static final long serialVersionUID = 1L;

	private final Map<String, String> errorMap;
	public ParamException(Map<String, String> errorMap) {
		super(A.isEmpty(errorMap) ? "" : Joiner.on("; ").join(new LinkedHashSet<>(errorMap.values())));
		this.errorMap = A.isEmpty(errorMap) ? Collections.emptyMap() : errorMap;
	}
}
