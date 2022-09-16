package com.github.global.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.global.query.util.QueryUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ReqResultGroup {

    COUNT("COUNT(%s)", "cnt", "总条数"),
    COUNT_DISTINCT("COUNT(DISTINCT %s)", "cnt%s", "总条数(去重)"),
    SUM("SUM(%s)", "sum%s", "总和"),
    MIN("MIN(%s)", "min%s", "最小"),
    MAX("MAX(%s)", "max%s", "最大"),
    AVG("AVG(%s)", "avg%s", "平均"),
    GROUP_CONCAT("GROUP_CONCAT(%s)", "gct%s", "组拼接");

    private final String value;
    private final String alias;
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

    public String generateAlias(String column) {
        return String.format(alias, "_" + column.replace(" ", "").replace(",", "__").replace(".", "_"));
    }

    public boolean checkHavingValue(Object value) {
        return (this == GROUP_CONCAT) ? (value instanceof String) : QueryUtil.isNumber(value);
    }

    public String havingField(List<?> groups) {
        if (groups == null || groups.size() < 2) {
            return "";
        }
        String column = QueryUtil.toStr(groups.get(1));
        if (column.isEmpty()) {
            return "";
        }
        return generateAlias(column);
    }
}
