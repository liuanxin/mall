package com.github.global.query.model;

import com.github.common.json.JsonUtil;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.util.QueryUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * <pre>
 * select id, orderNo from t_order ...
 * select id, address, phone from t_order_address ...
 * select id, name, price from t_order_item ...
 * {
 *   -- "scheme": "order",   -- 忽略将从 requestInfo 中获取
 *   "columns": [
 *     "id",
 *     "orderNo",
 *     {
 *       "address": {
 *         "scheme": "orderAddress",
 *         "columns": [ "id", "address", "phone" ]
 *       },
 *       "items": {
 *         "scheme": "orderItem",
 *         "columns": [ "id", "name", "price" ]
 *       }
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
            Set<String> columnSet = new HashSet<>();
            List<Object> innerList = new ArrayList<>();
            for (Object obj : columns) {
                if (obj != null) {
                    if (obj instanceof String column) {
                        if (!column.isEmpty()) {
                            QueryUtil.checkSchemeAndColumnName(currentScheme, column, columnInfo, "result select");
                            if (columnSet.contains(column)) {
                                throw new RuntimeException("res column(" + column + ") has repeated");
                            }
                            columnSet.add(column);
                        }
                    } else if (obj instanceof List<?> groups) {
                        if (!groups.isEmpty()) {
                            int size = groups.size();
                            if (size < 2) {
                                throw new RuntimeException("res function(" + groups + ") error");
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
                        innerList.add(obj);
                    }
                }
            }
            for (Object obj : innerList) {
                Map<String, ReqResult> inner = JsonUtil.convertType(obj, QueryConst.RESULT_TYPE);
                if (inner == null) {
                    throw new RuntimeException("res relation(" + obj + ") error");
                }
                for (Map.Entry<String, ReqResult> entry : inner.entrySet()) {
                    String column = entry.getKey();
                    if (columnSet.contains(column)) {
                        throw new RuntimeException("res relation column(" + column + ") has repeated");
                    }
                    ReqResult innerResult = entry.getValue();
                }
            }
        }
    }

    public Set<String> allResultScheme(String mainScheme) {
        Set<String> set = new LinkedHashSet<>();
        String currentScheme = (scheme == null || scheme.trim().isEmpty()) ? mainScheme : scheme.trim();
        if (columns != null && !columns.isEmpty()) {
            StringJoiner sj = new StringJoiner(", ");
            for (Object obj : columns) {
                if (obj != null) {
                    if (obj instanceof String column) {
                        set.add(QueryUtil.getSchemeName(column, mainScheme));
                    } else if (obj instanceof List<?> groups) {
                        if (!groups.isEmpty()) {
                            set.add(QueryUtil.getSchemeName(QueryUtil.toStr(groups.get(1)), mainScheme));
                        }
                    } else {
                        Map<String, ReqResult> inner = JsonUtil.convertType(obj, QueryConst.RESULT_TYPE);
                        if (inner != null) {
                            for (ReqResult innerResult : inner.values()) {
                                set.addAll(innerResult.allResultScheme(currentScheme));
                            }
                        }
                    }
                }
            }
        }
        return set;
    }

    public String generateSelectSql(String mainScheme, TableColumnInfo columnInfo) {
        if (columns != null && !columns.isEmpty()) {
            StringJoiner sj = new StringJoiner(", ");
            for (Object obj : columns) {
                if (obj instanceof String column) {
                    if (!column.isEmpty()) {
                        sj.add(QueryUtil.checkSchemeAndColumnName(mainScheme, column, columnInfo, "result select").getName());
                    }
//                } else { // todo

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
