package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReqParamOperateType {

    AND("and", "并且"),
    OR("or", "或者");

    private final String value;
    private final String msg;
    ReqParamOperateType(String value, String msg) {
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
    public static ReqParamOperateType deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (ReqParamOperateType e : values()) {
                if (str.equalsIgnoreCase(e.name()) || str.equalsIgnoreCase(e.value)) {
                    return e;
                }
            }
        }
        return null;
    }
}
