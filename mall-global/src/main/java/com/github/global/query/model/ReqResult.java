package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.common.json.JsonUtil;
import com.github.global.query.util.QueryUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * <pre>
 * select id, orderNo from order ...
 * select id, address, phone from orderAddress ...
 * {
 *   -- "scheme": "order",   -- 忽略将从 requestInfo 中获取
 *   "columns": [
 *     "id",
 *     "orderNo",
 *     {
 *       "scheme": "orderAddress",
 *       columns: [ "id", "address", "phone" ]
 *     }
 *   ]
 * }
 *
 *
 * select name, count(*) as cnt, min(id) as mid from ... group by name
 * {
 *   "columns": [
 *     "name",
 *     [ "count", "*", "cnt" ],
 *     [ "min", "id", "mid" ]
 *   ]
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqResult {

    private String scheme;
    /** 结构里的列 */
    private List<Object> columns;


    public void checkResult(String mainScheme, TableColumnInfo columnInfo) {
        String currentScheme = (scheme == null || scheme.trim().isEmpty()) ? mainScheme : scheme.trim();
        if (currentScheme == null || currentScheme.isEmpty()) {
            throw new RuntimeException("res need scheme");
        }

        Map<String, Scheme> schemeMap = columnInfo.getSchemeMap();
        if (!schemeMap.containsKey(currentScheme)) {
            throw new RuntimeException("no scheme(" + currentScheme + ") defined");
        }

        if (columns != null && !columns.isEmpty()) {
            StringJoiner sj = new StringJoiner(", ");
            for (Object obj : columns) {
                if (obj != null) {
                    if (obj instanceof String column) {
                        if (!column.isEmpty()) {
                            QueryUtil.checkSchemeAndColumnName(currentScheme, column, columnInfo, "result select");
                        }
                    } else if (obj instanceof List<?> groups) {
                        if (!groups.isEmpty()) {
                            int size = groups.size();
                            if (size < 2) {
                                throw new RuntimeException("result function(" + groups + ") error");
                            }
                            ReqResultGroup group = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(0)));
                            String column = QueryUtil.toStr(groups.get(1));

                            String checkType = "result function(" + group.name().toLowerCase() + ")";
                            if (group == ReqResultGroup.COUNT) {
                                if (!Set.of("*", "1", "0").contains(column)) {
                                    QueryUtil.checkColumnName(column, currentScheme, columnInfo, checkType);
                                }
                            } else {
                                QueryUtil.checkColumnName(column, currentScheme, columnInfo, checkType);
                            }
                        }
                    } else {
                        ReqResult inner = JsonUtil.convert(obj, ReqResult.class);
                        if (inner == null) {
                            throw new RuntimeException("relation result(" + obj + ") error");
                        }
                        inner.checkResult(currentScheme, columnInfo);
                    }
                }
            }
        }
    }

    public String generateSelectSql(String mainScheme, TableColumnInfo columnInfo) {
        if (columns != null && !columns.isEmpty()) {
            StringJoiner sj = new StringJoiner(", ");
            for (Object obj : columns) {
                if (obj instanceof String column) {
                    if (!column.isEmpty()) {
                        sj.add(QueryUtil.checkSchemeAndColumnName(mainScheme, column, columnInfo, "result select").getName());
                    }
//                } else {

                }
            }
            return sj.toString();
        }
        return "";
    }

//    public String generateFunctionSql(String mainScheme, TableColumnInfo columnInfo) {
//        if (functions != null && !functions.isEmpty()) {
//            StringJoiner sj = new StringJoiner(", ");
//            for (List<String> groups : functions) {
//                if (groups != null && !groups.isEmpty()) {
//                    int size = groups.size();
//                    if (size < 2) {
//                        throw new RuntimeException("function(" + groups + ") error");
//                    }
//                    ReqResultGroup group = ReqResultGroup.deserializer(groups.get(0).trim());
//                    String column = groups.get(1).trim();
//
//                    String checkType = "function(" + group.name().toLowerCase() + ")";
//                    if (group == ReqResultGroup.COUNT) {
//                        if (!Set.of("*", "1", "0").contains(column)) {
//                            QueryUtil.checkColumnName(column, mainScheme, columnInfo, checkType);
//                        }
//                    } else {
//                        QueryUtil.checkColumnName(column, mainScheme, columnInfo, checkType);
//                    }
//                    if (size > 2) {
//                        sj.add(String.format(group.getValue(), column) + " AS " + groups.get(2).trim());
//                    } else {
//                        sj.add(String.format(group.getValue(), column));
//                    }
//                }
//            }
//            return sj.toString();
//        }
//        return "";
//    }

    public String generateGroupSql(String functionSql, String mainScheme, TableColumnInfo columnInfo) {
        if (functionSql != null && !functionSql.trim().isEmpty()) {
            StringJoiner groupSj = new StringJoiner(", ");
            for (Object obj : columns) {
                if (obj instanceof String column) {
                    groupSj.add(column);
                }
            }
            String groupBy = groupSj.toString();
            if (!groupBy.isEmpty()) {
                return " GROUP BY " + groupBy;
            }
        }
        return "";
    }
}
