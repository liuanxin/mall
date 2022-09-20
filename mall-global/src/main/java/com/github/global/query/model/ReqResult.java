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
 *   name,
 *   COUNT(*),
 *   COUNT(distinct name, name2),
 *   SUM(price),
 *   MIN(id),
 *   MAX(id),
 *   AVG(price),
 *   GROUP_CONCAT(name)
 * FROM ...
 * GROUP BY name
 * HAVING SUM(price) > 100.5 AND SUM(price) < 120.5
 * {
 *   "columns": [
 *     "name",
 *     [ "count", "*", "x" ],
 *     [ "count_distinct", "name, name2", "xx" ],
 *     [ "sum", "price", "xxx", "gt", 100.5, "lt", 120.5 ],
 *     [ "min", "id", "y" ],
 *     [ "max", "id", "yy" ],
 *     [ "avg", "price", "yyy" ],
 *     [ "group_concat", "name", "z" ]
 *   ]
 * }
 * 第三个参数表示接口响应回去时的属性, 每四个和第五个参数表示 HAVING 过滤时的条件
 * 只支持基于 HAVING 进行 AND 条件(a > 1 AND a < 10)的过滤
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReqResult {

    /** 结构 */
    private String table;
    /** 结构里的列 */
    private List<Object> columns;


    public Set<String> checkResult(String mainTable, TableColumnInfo tableColumnInfo) {
        String currentTable = (table == null || table.trim().isEmpty()) ? mainTable : table.trim();
        Table tableInfo = tableColumnInfo.findTable(currentTable);
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
                    Table te = tableColumnInfo.findTable(QueryUtil.getTableName(col, currentTable));
                    if (te == null) {
                        throw new RuntimeException("result table(" + currentTable + ") column(" + col + ") has no defined table");
                    }
                    if (tableColumnInfo.findTableColumn(te, QueryUtil.getColumnName(col)) == null) {
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
                    String column = QueryUtil.toStr(groups.get(1));
                    if (column.isEmpty()) {
                        throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") column error");
                    }

                    ReqResultGroup group = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(0)));
                    if (group == null) {
                        throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") type error");
                    }
                    if (group == ReqResultGroup.COUNT_DISTINCT) {
                        for (String col : column.split(",")) {
                            Table te = tableColumnInfo.findTable(QueryUtil.getTableName(col.trim(), currentTable));
                            if (te == null) {
                                throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") has no defined table");
                            }
                            if (tableColumnInfo.findTableColumn(te, QueryUtil.getColumnName(col.trim())) == null) {
                                throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") has no defined column");
                            }
                            resultFunctionTableSet.add(te.getName());
                        }
                    } else {
                        if (!(group == ReqResultGroup.COUNT && Set.of("*", "1").contains(column))) {
                            Table te = tableColumnInfo.findTable(QueryUtil.getTableName(column, currentTable));
                            if (te == null) {
                                throw new RuntimeException("result table(" + currentTable + ") function(" + groups + ") has no defined table");
                            }
                            if (tableColumnInfo.findTableColumn(te, QueryUtil.getColumnName(column)) == null) {
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
                            if (group.checkHavingValue(value)) {
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
                if (tableColumnInfo.findRelationByMasterChild(currentTable, innerTable) == null) {
                    throw new RuntimeException("result " + currentTable + " - " + column + " -" + innerTable + " has no relation");
                }
                innerResult.checkResult(innerTable, tableColumnInfo);
            }
        }
        return resultFunctionTableSet;
    }

    public String generateSelectSql(String mainTable, boolean needAlias, TableColumnInfo tableColumnInfo,
                                    Set<String> firstQueryTableSet) {
        String currentTableName = (table == null || table.trim().isEmpty()) ? mainTable : table.trim();
        StringJoiner sj = new StringJoiner(", ");
        Set<String> columnNameSet = new HashSet<>();

        for (Object obj : columns) {
            if (obj instanceof String column) {
                if (!column.isEmpty()) {
                    String tableName = QueryUtil.getTableName(column, currentTableName);
                    if (tableName.equals(currentTableName) || firstQueryTableSet.contains(tableName)) {
                        String addKey = tableName + "." + QueryUtil.getColumnName(column);
                        if (!columnNameSet.contains(addKey)) {
                            sj.add(QueryUtil.getUseColumn(needAlias, column, mainTable, tableColumnInfo));
                        }
                        columnNameSet.add(addKey);
                    }
                }
            } else if (!(obj instanceof List<?>)) {
                Map<String, ReqResult> inner = JsonUtil.convertType(obj, QueryConst.RESULT_TYPE);
                for (ReqResult innerResult : inner.values()) {
                    String innerTable = innerResult.getTable();
                    TableColumnRelation relation = tableColumnInfo.findRelationByMasterChild(currentTableName, innerTable);
                    String addKey = relation.getOneTable() + "." + relation.getOneColumn();
                    if (!columnNameSet.contains(addKey)) {
                        sj.add(QueryUtil.getUseColumn(needAlias, relation.getOneColumn(), currentTableName, tableColumnInfo));
                    }
                    columnNameSet.add(addKey);
                }
            }
        }
        return sj.toString();
    }

    public String generateOtherSelectSql() {
        // todo
        return null;
    }
    public String generateOtherFromSql() {
        return null;
    }

    public String generateFunctionSql(String mainTable, boolean needAlias, TableColumnInfo tableColumnInfo) {
        StringJoiner sj = new StringJoiner(", ");
        for (Object obj : columns) {
            if (obj instanceof List<?> groups) {
                if (!groups.isEmpty()) {
                    String column = QueryUtil.toStr(groups.get(1));
                    if (!column.isEmpty()) {
                        ReqResultGroup group = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(0)));
                        if (group == ReqResultGroup.COUNT_DISTINCT) {
                            StringJoiner funSj = new StringJoiner(", ");
                            for (String col : column.split(",")) {
                                funSj.add(QueryUtil.getUseColumn(needAlias, col.trim(), mainTable, tableColumnInfo));
                            }
                            sj.add(group.generateColumn(funSj.toString()));
                        } else {
                            sj.add(group.generateColumn(QueryUtil.getUseColumn(needAlias, column, mainTable, tableColumnInfo)));
                        }
                    }
                }
            }
        }
        return sj.toString();
    }

    public boolean notNeedGroup() {
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
        return !(hasColumn && hasGroup);
    }
    public String generateGroupSql(String mainTable, boolean needAlias, TableColumnInfo tableColumnInfo) {
        StringJoiner sj = new StringJoiner(", ");
        boolean hasGroup = false;
        for (Object obj : columns) {
            if (obj instanceof String column) {
                if (!column.isEmpty()) {
                    sj.add(QueryUtil.getUseColumn(needAlias, column, mainTable, tableColumnInfo));
                }
            } else if (obj instanceof List<?> groups) {
                if (!groups.isEmpty()) {
                    hasGroup = true;
                }
            }
        }
        return (hasGroup && sj.length() > 0) ? (" GROUP BY " + sj) : "";
    }

    public String generateHavingSql(String mainTable, boolean needAlias, TableColumnInfo tableColumnInfo, List<Object> params) {
        StringJoiner groupSj = new StringJoiner(" AND ");
        for (Object obj : columns) {
            if (obj instanceof List<?> groups) {
                if (!groups.isEmpty()) {
                    int size = groups.size();
                    if (size > 4) {
                        String column = QueryUtil.toStr(groups.get(1));
                        if (!column.isEmpty()) {
                            ReqResultGroup group = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(0)));
                            String useColumn = QueryUtil.getUseColumn(needAlias, column, mainTable, tableColumnInfo);
                            String havingColumn = group.generateColumn(useColumn);

                            String tableName = QueryUtil.getTableName(column, mainTable);
                            String columnName = QueryUtil.getColumnName(column);
                            Class<?> columnType = tableColumnInfo.findTableColumn(tableName, columnName).getColumnType();
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
            }
        }
        String groupBy = groupSj.toString();
        return groupBy.isEmpty() ? "" : (" HAVING " + groupBy);
    }
}
