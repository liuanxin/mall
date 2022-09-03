package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ReqResultFunction {

    COUNT("COUNT(*)", "条数"),
    SUM("SUM(%s)", "总数"),
    GROUP_CONCAT("GROUP_CONCAT(%s)", "组拼接"),
    ;


    @JsonValue
    private final String value;
    private final String msg;

    @JsonCreator
    public static ReqResultFunction deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (ReqResultFunction e : values()) {
                if (str.equalsIgnoreCase(e.name()) || str.equalsIgnoreCase(e.value)) {
                    return e;
                }
            }
        }
        return null;
    }
}
