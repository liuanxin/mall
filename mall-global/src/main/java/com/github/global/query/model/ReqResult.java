package com.github.global.query.model;

import com.github.common.json.JsonUtil;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.enums.ReqParamConditionType;
import com.github.global.query.enums.ReqResultGroup;
import com.github.global.query.util.QueryUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * <pre>
 * 1. FROM & JOINs: determine & filter rows
 * 2. WHERE: more filters on the rows
 * 3. GROUP BY: combines those rows into groups
 * 4. HAVING: filters groups
 * 5. ORDER BY: arranges the remaining rows/groups
 * 6. LIMIT: filters on the remaining rows/groups
 *
 * SELECT id, order_no FROM t_order ...
 * SELECT id, address, phone FROM t_order_address ...
 * SELECT id, name, price FROM t_order_item ...
 * {
 *   "columns": [
 *     "id",
 *     "orderNo",
 *     {
 *       "address": {
 *         "table": "orderAddress",
 *         "columns": [ "id", "address", "phone" ]
 *       },
 *       "items": {
 *         "table": "orderItem",
 *         "columns": [ "id", "name", "price" ]
 *       }
 *     }
 *   ]
 * }
 *
 *
 * COUNT(*) 跟 COUNT(1) 是等价的, 使用标准 COUNT(*) 即可
 * 见: https://dev.mysql.com/doc/refman/8.0/en/aggregate-functions.html#function_count
 *
 * SELECT
 *   name, COUNT(*), COUNT(DISTINCT name, name2), SUM(price),
 *   MIN(id), MAX(id), AVG(price), GROUP_CONCAT(name)
 * ...
 * GROUP BY name
 * HAVING  SUM(price) > 100.5  AND  SUM(price) < 120.5  AND  GROUP_CONCAT(name) LIKE 'aaa%'
 * {
 *   "columns": [
 *     "name",
 *     [ "abc", "count", "*" ],
 *     [ "def", "count_distinct", "name, name2" ],
 *     [ "ghi", "sum", "price", "gt", 100.5, "lt", 120.5 ],
 *     [ "jkl", "min", "id" ],
 *     [ "mno", "max", "id" ],
 *     [ "pqr", "avg", "price" ],
 *     [ "stu", "group_concat", "name", "lks", "aaa" ]
 *   ]
 * }
 * 第一个参数表示接口响应回去时的属性,
 * 每四个和第五个参数表示 HAVING 过滤时的条件, 只支持 AND 条件过滤, 复杂的嵌套没有想到好的抽象方式
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReqResult {

    /** 表 */
    private String table;
    /** 表里的列 */
    private List<Object> columns;


    public Set<String> checkResult(String mainTable, TableColumnInfo tcInfo) {
        String currentTable = (table == null || table.trim().isEmpty()) ? mainTable : table.trim();
        Table tableInfo = tcInfo.findTable(currentTable);
        if (tableInfo == null) {
            throw new RuntimeException("no result table(" + currentTable + ") defined");
        }
        if (columns == null || columns.isEmpty()) {
            throw new RuntimeException("result table(" + currentTable + ") need columns");
        }

        Set<String> resultFunctionTableSet = new LinkedHashSet<>();
        Set<String> columnCheckRepeatedSet = new HashSet<>();
        List<Object> innerList = new ArrayList<>();
        boolean hasColumnOrFunction = false;
        for (Object obj : columns) {
            if (obj != null) {
                if (obj instanceof String column) {
                    if (column.trim().isEmpty()) {
                        throw new RuntimeException("result table(" + currentTable + ") column can't be blank");
                    }

                    String col = column.trim();
                    Table te = tcInfo.findTable(QueryUtil.getTableName(col, currentTable));
                    if (te == null) {
                        throw new RuntimeException("result table(" + currentTable + ") column(" + col + ") has no defined table");
                    }
                    if (tcInfo.findTableColumn(te, QueryUtil.getColumnName(col)) == null) {
                        throw new RuntimeException("result table(" + currentTable + ") column(" + col + ") has no defined column");
                    }

                    if (columnCheckRepeatedSet.contains(col)) {
                        throw new RuntimeException("result table(" + currentTable + ") column(" + col + ") has repeated");
                    }
                    columnCheckRepeatedSet.add(col);
                    hasColumnOrFunction = true;
                } else if (obj instanceof List<?> groups) {
                    if (groups.isEmpty()) {
                        throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") error");
                    }
                    int size = groups.size();
                    if (size < 3) {
                        throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") data error");
                    }
                    ReqResultGroup group = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(1)));
                    if (group == null) {
                        throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") type error");
                    }
                    String column = QueryUtil.toStr(groups.get(2));
                    if (column.isEmpty()) {
                        throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") column error");
                    }

                    if (group == ReqResultGroup.COUNT_DISTINCT) {
                        for (String col : column.split(",")) {
                            Table te = tcInfo.findTable(QueryUtil.getTableName(col.trim(), currentTable));
                            if (te == null) {
                                throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") has no defined table");
                            }
                            if (tcInfo.findTableColumn(te, QueryUtil.getColumnName(col.trim())) == null) {
                                throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") has no defined column");
                            }
                            resultFunctionTableSet.add(te.getName());
                        }
                    } else {
                        if (!(group == ReqResultGroup.COUNT && Set.of("*", "1").contains(column))) {
                            Table te = tcInfo.findTable(QueryUtil.getTableName(column, currentTable));
                            if (te == null) {
                                throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") has no defined table");
                            }
                            if (tcInfo.findTableColumn(te, QueryUtil.getColumnName(column)) == null) {
                                throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") has no defined column");
                            }
                            resultFunctionTableSet.add(te.getName());
                        }
                    }

                    String functionColumn = group.generateColumn(column);
                    if (columnCheckRepeatedSet.contains(functionColumn)) {
                        throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") has repeated");
                    }
                    columnCheckRepeatedSet.add(functionColumn);

                    if (size > 4) {
                        // 先右移 1 位除以 2, 再左移 1 位乘以 2, 变成偶数
                        int evenSize = size >> 1 << 1;
                        for (int i = 3; i < evenSize; i += 2) {
                            ReqParamConditionType conditionType = ReqParamConditionType.deserializer(groups.get(i));
                            if (conditionType == null) {
                                throw new RuntimeException("result table(" + currentTable + ") function("
                                        + groups + ") having condition error");
                            }

                            Object value = groups.get(i + 1);
                            if (group.checkNotHavingValue(value)) {
                                throw new RuntimeException("result table(" + currentTable + ") function("
                                        + groups + ") having condition value(" + value + ") type error");
                            }
                        }
                    }
                    hasColumnOrFunction = true;
                } else {
                    innerList.add(obj);
                }
            }
        }
        if (!hasColumnOrFunction) {
            throw new RuntimeException("result table(" + currentTable + ") no columns");
        }

        for (Object obj : innerList) {
            Map<String, ReqResult> inner = JsonUtil.convertType(obj, QueryConst.RESULT_TYPE);
            if (inner == null) {
                throw new RuntimeException("result table(" + currentTable + ") relation(" + obj + ") error");
            }
            for (Map.Entry<String, ReqResult> entry : inner.entrySet()) {
                String column = entry.getKey();
                ReqResult innerResult = entry.getValue();
                if (innerResult == null) {
                    throw new RuntimeException("result table(" + currentTable + ") relation column(" + column + ") error");
                }
                if (columnCheckRepeatedSet.contains(column)) {
                    throw new RuntimeException("result table(" + currentTable + ") relation column(" + column + ") has repeated");
                }
                columnCheckRepeatedSet.add(column);
                String innerTable = innerResult.getTable();
                if (innerTable == null || innerTable.isEmpty()) {
                    throw new RuntimeException("result table(" + currentTable + ") inner(" + column + ") need table");
                }
                if (tcInfo.findRelationByMasterChild(currentTable, innerTable) == null) {
                    throw new RuntimeException("result " + currentTable + " - " + column + " -" + innerTable + " has no relation");
                }
                innerResult.checkResult(innerTable, tcInfo);
            }
        }
        return resultFunctionTableSet;
    }

    public String generateSelectSql(String mainTable, TableColumnInfo tcInfo, Set<String> tableSet) {
        String currentTableName = (table == null || table.isEmpty()) ? mainTable : table.trim();
        Set<String> columnNameSet = new LinkedHashSet<>();
        boolean needAlias = !tableSet.isEmpty();
        if (needAlias) {
            columnNameSet.add(tcInfo.findTable(mainTable).idSelect(true));
        }

        for (Object obj : columns) {
            if (obj instanceof String column) {
                String col = column.trim();
                String tableName = QueryUtil.getTableName(col, currentTableName);
                if (tableName.equals(currentTableName) || tableSet.contains(tableName)) {
                    columnNameSet.add(QueryUtil.getUseColumn(needAlias, col, mainTable, tcInfo));
                }
            }
        }
        columnNameSet.addAll(innerColumn(mainTable, tcInfo, !tableSet.isEmpty()));
        return String.join(", ", columnNameSet);
    }

    public Set<String> innerColumn(String mainTable, TableColumnInfo tcInfo, boolean needAlias) {
        Set<String> columnNameSet = new LinkedHashSet<>();
        String currentTableName = (table == null || table.isEmpty()) ? mainTable : table.trim();
        for (Object obj : columns) {
            if (!(obj instanceof String)  && !(obj instanceof List<?>)) {
                Map<String, ReqResult> inner = JsonUtil.convertType(obj, QueryConst.RESULT_TYPE);
                for (ReqResult innerResult : inner.values()) {
                    String innerTable = innerResult.getTable();
                    TableColumnRelation relation = tcInfo.findRelationByMasterChild(currentTableName, innerTable);
                    columnNameSet.add(QueryUtil.getUseColumn(needAlias, relation.getOneColumn(), currentTableName, tcInfo));
                }
            }
        }
        return columnNameSet;
    }

    public String generateFunctionSql(String mainTable, boolean needAlias, TableColumnInfo tcInfo) {
        StringJoiner sj = new StringJoiner(", ");
        for (Object obj : columns) {
            if (obj instanceof List<?> groups) {
                String column = QueryUtil.toStr(groups.get(2));
                ReqResultGroup group = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(1)));
                if (group == ReqResultGroup.COUNT_DISTINCT) {
                    StringJoiner funSj = new StringJoiner(", ");
                    for (String col : column.split(",")) {
                        funSj.add(QueryUtil.getUseColumn(needAlias, col.trim(), mainTable, tcInfo));
                    }
                    sj.add(group.generateColumn(funSj.toString()));
                } else {
                    sj.add(group.generateColumn(QueryUtil.getUseColumn(needAlias, column, mainTable, tcInfo)));
                }
            }
        }
        return sj.toString();
    }

    public boolean needGroup() {
        boolean hasColumn = false;
        boolean hasGroup = false;
        for (Object obj : columns) {
            if (obj instanceof String column) {
                if (!column.isEmpty()) {
                    hasColumn = true;
                }
            } else if (obj instanceof List<?> groups) {
                if (!groups.isEmpty()) {
                    hasGroup = true;
                }
            }
        }
        return hasColumn && hasGroup;
    }
    public String generateGroupSql(String mainTable, boolean needAlias, TableColumnInfo tcInfo) {
        StringJoiner sj = new StringJoiner(", ");
        boolean hasGroup = false;
        for (Object obj : columns) {
            if (obj instanceof String column) {
                if (!column.isEmpty()) {
                    sj.add(QueryUtil.getUseColumn(needAlias, column, mainTable, tcInfo));
                }
            } else if (obj instanceof List<?> groups) {
                if (!groups.isEmpty()) {
                    hasGroup = true;
                }
            }
        }
        return (hasGroup && sj.length() > 0) ? (" GROUP BY " + sj) : "";
    }

    public String generateHavingSql(String mainTable, boolean needAlias, TableColumnInfo tcInfo, List<Object> params) {
        StringJoiner groupSj = new StringJoiner(" AND ");
        for (Object obj : columns) {
            if (obj instanceof List<?> groups) {
                int size = groups.size();
                if (size > 4) {
                    String column = QueryUtil.toStr(groups.get(2));
                    ReqResultGroup group = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(1)));
                    String useColumn = QueryUtil.getUseColumn(needAlias, column, mainTable, tcInfo);
                    String havingColumn = group.generateColumn(useColumn);

                    String tableName = QueryUtil.getTableName(column, mainTable);
                    String columnName = QueryUtil.getColumnName(column);
                    Class<?> columnType = tcInfo.findTableColumn(tableName, columnName).getColumnType();
                    // 先右移 1 位除以 2, 再左移 1 位乘以 2, 变成偶数
                    int evenSize = size >> 1 << 1;
                    for (int i = 3; i < evenSize; i += 2) {
                        ReqParamConditionType conditionType = ReqParamConditionType.deserializer(groups.get(i));
                        Object value = groups.get(i + 1);

                        String sql = conditionType.generateSql(havingColumn, columnType, value, params);
                        if (!sql.isEmpty()) {
                            groupSj.add(sql);
                        }
                    }
                }
            }
        }
        String groupBy = groupSj.toString();
        return groupBy.isEmpty() ? "" : (" HAVING " + groupBy);
    }
}
