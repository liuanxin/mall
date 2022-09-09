package com.github.global.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.global.query.util.QuerySqlUtil;
import com.github.global.query.util.QueryUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ReqResultGroup {

    COUNT("COUNT(%s)", "`_cnt%s`", "总条数"),
    SUM("SUM(%s)", "`_sum%s`", "总和"),
    MIN("MIN(%s)", "`_min%s`", "最小"),
    MAX("MAX(%s)", "`_max%s`", "最大"),
    AVG("AVG(%s)", "`_avg%s`", "平均"),
    GCT("GROUP_CONCAT(%s)", "`_gct%s`", "组拼接");

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

    public String generateSelectFunction(List<?> groups) {
        if (groups == null) {
            return "";
        }
        int size = groups.size();
        if (size < 2) {
            return "";
        }

        String column = QueryUtil.toStr(groups.get(1));
        if (column.isEmpty()) {
            return "";
        }

        String funcValue = String.format(value, QuerySqlUtil.toSqlField(column));
        String funcAlias = String.format(alias, ("*".equals(column) ? "" : ("_" + column)));
        return funcValue + " AS " + funcAlias;
    }

    public boolean checkHavingValue(Object value) {
        return (this == GCT) ? (value instanceof String) : QueryUtil.isNumber(value);
    }

    public String havingField(List<?> groups) {
        if (groups == null) {
            return "";
        }
        String column = QueryUtil.toStr(groups.get(1));
        if (column.isEmpty()) {
            return "";
        }
        return String.format(alias, ("*".equals(column) ? "" : ("_" + column)));
    }
}
