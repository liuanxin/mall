package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReqResultGroup {

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
    public static ReqResultGroup deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (ReqResultGroup e : values()) {
                if (str.equalsIgnoreCase(e.name())) {
                    return e;
                }
            }
        }
        return null;
    }
}
