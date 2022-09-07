package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReqResultType {

    OBJ("对象"),
    ARR("数组");

    private final String msg;

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static ReqResultType deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (ReqResultType e : values()) {
                if (str.equalsIgnoreCase(e.name())) {
                    return e;
                }
            }
        }
        return null;
    }
}
