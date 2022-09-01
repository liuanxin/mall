package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReqResultFunction {

    COUNT("COUNT(*)", "条数"),
    SUM("SUM(%s)", "总数"),
    GROUP_CONCAT("GROUP_CONCAT(%s)", "组拼接"),
    ;

    private final String value;
    private final String msg;
    ReqResultFunction(String value, String msg) {
        this.value = value;
        this.msg = msg;
    }
    @JsonValue
    public String getValue() {
        return value;
    }
    public String getMsg() {
        return msg;
    }

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
