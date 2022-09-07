package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.global.query.util.MysqlKeyWordUtil;
import com.github.global.query.util.QueryUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ReqResultGroup {

    COUNT("COUNT(%s) AS `cnt_%s`", "总条数"),
    SUM("SUM(%s) AS sum_%s", "总和"),
    MIN("MIN(%s) AS min_%s", "最小"),
    MAX("MAX(%s) AS max_%s", "最大"),
    AVG("AVG(%s) AS avg_%s", "平均"),
    GCT("GROUP_CONCAT(%s) AS gct_%s", "组拼接");


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

    public String generateSql(List<?> groups) {
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

        return String.format(value, MysqlKeyWordUtil.toSql(column), column);
    }
}
