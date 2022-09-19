package com.github.global.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReqJoinType {

    INNER("内联"),
    LEFT("左联"),
    RIGHT("右联");

    private final String msg;


    @JsonValue
    public String value() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static ReqJoinType deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (ReqJoinType e : values()) {
                if (str.equalsIgnoreCase(e.name())) {
                    return e;
                }
            }
        }
        return null;
    }
}
