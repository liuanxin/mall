package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReqParamJoinType {

    LEFT("左联"),
    RIGHT("右连");

    private final String msg;

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static ReqParamJoinType deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (ReqParamJoinType e : values()) {
                if (str.equalsIgnoreCase(e.name())) {
                    return e;
                }
            }
        }
        return null;
    }
}
