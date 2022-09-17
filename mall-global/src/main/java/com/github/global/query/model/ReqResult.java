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
 *         "schema": "orderAddress",
 *         "columns": [ "id", "address", "phone" ]
 *       },
 *       "items": {
 *         "schema": "orderItem",
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
    private String schema;
    /** 结构里的列 */
    private List<Object> columns;


    public Set<String> checkResult(String mainSchema, SchemaColumnInfo schemaColumnInfo, Set<String> paramSchemaSet) {
        String currentSchema = (schema == null || schema.trim().isEmpty()) ? mainSchema : schema.trim();
        Schema schemaInfo = schemaColumnInfo.findSchema(currentSchema);
        if (schemaInfo == null) {
            throw new RuntimeException("no result schema(" + currentSchema + ") defined");
        }
        if (columns == null || columns.isEmpty()) {
            throw new RuntimeException("result schema(" + currentSchema + ") need columns");
        }

        Set<String> functionSchemaSet = new HashSet<>();
        Set<String> columnCheckRepeatedSet = new HashSet<>();
        List<Object> innerList = new ArrayList<>();
        boolean hasColumnOrFunction = false;
        for (Object obj : columns) {
            if (obj != null) {
                if (obj instanceof String column) {
                    if (column.trim().isEmpty()) {
                        throw new RuntimeException("result schema(" + currentSchema + ") column can't be blank");
                    }

                    String col = column.trim();
                    QueryUtil.checkColumnName(col, currentSchema, schemaColumnInfo, "result select");
                    if (columnCheckRepeatedSet.contains(col)) {
                        throw new RuntimeException("result schema(" + currentSchema + ") column(" + col + ") has repeated");
                    }
                    columnCheckRepeatedSet.add(col);
                    hasColumnOrFunction = true;
                } else if (obj instanceof List<?> groups) {
                    if (groups.isEmpty()) {
                        throw new RuntimeException("result schema(" + currentSchema + ") function(" + groups + ") error");
                    }
                    int size = groups.size();
                    if (size < 3) {
                        throw new RuntimeException("result schema(" + currentSchema + ") function(" + groups + ") error");
                    }
                    String column = QueryUtil.toStr(groups.get(1));
                    if (column.isEmpty()) {
                        throw new RuntimeException("result schema(" + currentSchema + ") function(" + groups + ") column error");
                    }

                    ReqResultGroup group = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(0)));
                    if (group == null) {
                        throw new RuntimeException("result schema(" + currentSchema + ") function(" + groups + ") type error");
                    }
                    String checkType = "result schema(" + currentSchema + ") function(" + group.name().toLowerCase() + ")";
                    if (group == ReqResultGroup.COUNT_DISTINCT) {
                        for (String col : column.split(",")) {
                            String schemaName = QueryUtil.getSchemaName(col.trim(), currentSchema);
                            if (!currentSchema.equals(schemaName) && !paramSchemaSet.contains(schemaName)) {
                                throw new RuntimeException("result schema(" + currentSchema + ") function("
                                        + groups + ") no column in param");
                            }
                            QueryUtil.checkColumnName(col.trim(), currentSchema, schemaColumnInfo, checkType);
                            functionSchemaSet.add(QueryUtil.getSchemaName(column, currentSchema));
                        }
                    } else {
                        if (!(group == ReqResultGroup.COUNT && Set.of("*", "1").contains(column))) {
                            String schemaName = QueryUtil.getSchemaName(column, currentSchema);
                            if (!currentSchema.equals(schemaName) && !paramSchemaSet.contains(schemaName)) {
                                throw new RuntimeException("result schema(" + currentSchema + ") function("
                                        + groups + ") no column in param");
                            }
                            QueryUtil.checkColumnName(column, currentSchema, schemaColumnInfo, checkType);
                            functionSchemaSet.add(QueryUtil.getSchemaName(column, currentSchema));
                        }
                    }
                    if (size > 4) {
                        // 先右移 1 位除以 2, 再左移 1 位乘以 2, 变成偶数
                        int evenSize = size >> 1 << 1;
                        for (int i = 3; i < evenSize; i += 2) {
                            ReqParamConditionType conditionType = ReqParamConditionType.deserializer(groups.get(i));
                            if (conditionType == null) {
                                throw new RuntimeException("result schema(" + currentSchema + ") function("
                                        + groups + ") having condition error");
                            }

                            Object value = groups.get(i + 1);
                            if (group.checkHavingValue(value)) {
                                throw new RuntimeException("result schema(" + currentSchema + ") function("
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
            throw new RuntimeException("result schema(" + currentSchema + ") no columns");
        }

        for (Object obj : innerList) {
            Map<String, ReqResult> inner = JsonUtil.convertType(obj, QueryConst.RESULT_TYPE);
            if (inner == null) {
                throw new RuntimeException("result schema(" + currentSchema + ") relation(" + obj + ") error");
            }
            for (Map.Entry<String, ReqResult> entry : inner.entrySet()) {
                String column = entry.getKey();
                ReqResult innerResult = entry.getValue();
                if (innerResult == null) {
                    throw new RuntimeException("result schema(" + currentSchema + ") relation column(" + column + ") error");
                }
                if (columnCheckRepeatedSet.contains(column)) {
                    throw new RuntimeException("result schema(" + currentSchema + ") relation column(" + column + ") has repeated");
                }
                columnCheckRepeatedSet.add(column);
                String innerSchema = innerResult.getSchema();
                if (innerSchema == null || innerSchema.isEmpty()) {
                    throw new RuntimeException("result schema(" + currentSchema + ") inner(" + column + ") need schema");
                }
                if (schemaColumnInfo.findRelationByMasterChild(currentSchema, innerSchema) == null) {
                    throw new RuntimeException("result " + currentSchema + " - " + column + " -" + innerSchema + " has no relation");
                }
                innerResult.checkResult(innerSchema, schemaColumnInfo, paramSchemaSet);
            }
        }
        return functionSchemaSet;
    }

    public void checkSchema(String mainSchema, Set<String> paramSchemaSet, Set<String> functionSchemaSet,
                            SchemaColumnInfo schemaColumnInfo) {
        if (!functionSchemaSet.isEmpty()) {
            functionSchemaSet.remove(mainSchema);
            for (String functionSchema : functionSchemaSet) {
                if (!paramSchemaSet.contains(functionSchema)) {
                    boolean hasRelation = false;
                    for (String paramSchema : paramSchemaSet) {
                        if (schemaColumnInfo.findRelationByMasterChild(paramSchema, functionSchema) != null) {
                            hasRelation = true;
                            break;
                        }
                    }
                    if (!hasRelation) {
                        throw new RuntimeException("result function schema(" + functionSchema + ") has no relation with param schema");
                    }
                    paramSchemaSet.add(functionSchema);
                }
            }
        }
    }

    public String generateSelectSql(String mainSchema, Set<String> paramSchema, SchemaColumnInfo schemaColumnInfo) {
        String currentSchemaName = (schema == null || schema.trim().isEmpty()) ? mainSchema : schema.trim();
        Schema schema = schemaColumnInfo.findSchema(currentSchemaName);
        StringJoiner sj = new StringJoiner(", ");
        Set<String> columnNameSet = new HashSet<>();

        boolean needAlias = !paramSchema.isEmpty();
        for (Object obj : columns) {
            if (obj instanceof String column) {
                if (!column.isEmpty()) {
                    String schemaName = QueryUtil.getSchemaName(column, currentSchemaName);
                    if (schemaName.equals(currentSchemaName) || paramSchema.contains(schemaName)) {
                        String addKey = schemaName + "." + QueryUtil.getColumnName(column);
                        if (!columnNameSet.contains(addKey)) {
                            sj.add(QueryUtil.getUseColumn(needAlias, column, mainSchema, schemaColumnInfo));
                        }
                        columnNameSet.add(addKey);
                    }
                }
            } else if (!(obj instanceof List<?>)) {
                Map<String, ReqResult> inner = JsonUtil.convertType(obj, QueryConst.RESULT_TYPE);
                for (ReqResult innerResult : inner.values()) {
                    String innerSchema = innerResult.getSchema();
                    SchemaColumnRelation relation = schemaColumnInfo.findRelationByMasterChild(currentSchemaName, innerSchema);
                    String addKey = relation.getOneSchema() + "." + relation.getOneColumn();
                    if (!columnNameSet.contains(addKey)) {
                        sj.add(QueryUtil.getUseColumn(needAlias, relation.getOneColumn(), currentSchemaName, schemaColumnInfo));
                    }
                    columnNameSet.add(addKey);
                }
            }
        }
        return sj.toString();
    }

    public String generateFunctionSql(String mainSchema, boolean needAlias, SchemaColumnInfo schemaColumnInfo) {
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
                                funSj.add(QueryUtil.getUseColumn(needAlias, col.trim(), mainSchema, schemaColumnInfo));
                            }
                            sj.add(group.generateColumn(funSj.toString()));
                        } else {
                            String useColumn = QueryUtil.getUseColumn(needAlias, column, mainSchema, schemaColumnInfo);
                            sj.add(group.generateColumn(useColumn));
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
    public String generateGroupSql(String mainSchema, boolean needAlias, SchemaColumnInfo schemaColumnInfo) {
        StringJoiner sj = new StringJoiner(", ");
        boolean hasGroup = false;
        for (Object obj : columns) {
            if (obj instanceof String column) {
                if (!column.isEmpty()) {
                    sj.add(QueryUtil.getUseColumn(needAlias, column, mainSchema, schemaColumnInfo));
                }
            } else if (obj instanceof List<?> groups) {
                if (!groups.isEmpty()) {
                    hasGroup = true;
                }
            }
        }
        return (hasGroup && sj.length() > 0) ? (" GROUP BY " + sj) : "";
    }

    public String generateHavingSql(String mainSchema, boolean needAlias, SchemaColumnInfo schemaColumnInfo, List<Object> params) {
        StringJoiner groupSj = new StringJoiner(" AND ");
        for (Object obj : columns) {
            if (obj instanceof List<?> groups) {
                if (!groups.isEmpty()) {
                    int size = groups.size();
                    if (size > 4) {
                        String column = QueryUtil.toStr(groups.get(1));
                        if (!column.isEmpty()) {
                            ReqResultGroup group = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(0)));
                            String useColumn = QueryUtil.getUseColumn(needAlias, column, mainSchema, schemaColumnInfo);
                            String havingColumn = group.generateColumn(useColumn);
                            // 先右移 1 位除以 2, 再左移 1 位乘以 2, 变成偶数
                            int evenSize = size >> 1 << 1;
                            for (int i = 3; i < evenSize; i += 2) {
                                ReqParamConditionType conditionType = ReqParamConditionType.deserializer(groups.get(i));
                                Object value = groups.get(i + 1);

                                String sql = conditionType.generateSql(havingColumn, value, params);
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
