package com.github.global.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.global.query.util.QueryUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReqResultGroup {

    COUNT("COUNT(%s)", "总条数"),
    COUNT_DISTINCT("COUNT(DISTINCT %s)", "总条数(去重)"),
    SUM("SUM(%s)", "总和"),
    MIN("MIN(%s)", "最小"),
    MAX("MAX(%s)", "最大"),
    AVG("AVG(%s)", "平均"),
    GROUP_CONCAT("GROUP_CONCAT(%s)", "组拼接");

    private final String value;
    private final String msg;

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }

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

    public String generateColumn(String column) {
        return String.format(value, column);
    }

    public boolean checkNotHavingValue(Object value) {
        if (this == COUNT || this == COUNT_DISTINCT) {
            // COUNT  SUM  function needs to be int
            return QueryUtil.isNotLong(value);
        } else if (this == SUM || this == MIN || this == MAX || this == AVG) {
            // MIN  MAX  AVG  function needs to be double
            return QueryUtil.isNotDouble(value);
        } else {
            return false;
        }
    }
}
