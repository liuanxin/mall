package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReqResultGroupFunction {

    COUNT("COUNT(%s)", "总条数"),
    SUM("SUM(%s)", "总和"),
    MIN("MIN(%s)", "最小"),
    MAX("MAX(%s)", "最大"),
    AVG("AVG(%s)", "平均"),
    GROUP_CONCAT("GROUP_CONCAT(%s)", "组拼接"),
    ;


    @JsonValue
    private final String value;
    private final String msg;

    @JsonCreator
    public static ReqResultGroupFunction deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (ReqResultGroupFunction e : values()) {
                if (str.equalsIgnoreCase(e.name()) || str.equalsIgnoreCase(e.value)) {
                    return e;
                }
            }
        }
        return null;
    }
}
