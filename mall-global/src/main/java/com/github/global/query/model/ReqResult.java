package com.github.global.query.model;

import com.github.common.json.JsonUtil;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.enums.ReqParamConditionType;
import com.github.global.query.enums.ReqResultGroup;
import com.github.global.query.util.QuerySqlUtil;
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
 *   COUNT(*) AS cnt,
 *   COUNT(distinct name, name2) AS cnt_name,   -- 别名是自动拼装的
 *   SUM(price) AS sum_price,
 *   MIN(id) AS min_id,
 *   MAX(id) AS max_id,
 *   AVG(price) AS avg_price,
 *   GROUP_CONCAT(name) AS gct_name
 * FROM ...
 * GROUP BY name
 * HAVING _sum_price > 100.5 AND _sum_price < 120.5
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


    public void checkResult(String mainSchema, SchemaColumnInfo schemaColumnInfo, Set<String> paramSchemaSet) {
        String currentSchema = (schema == null || schema.trim().isEmpty()) ? mainSchema : schema.trim();
        Schema schemaInfo = schemaColumnInfo.findSchema(currentSchema);
        if (schemaInfo == null) {
            throw new RuntimeException("no res schema(" + currentSchema + ") defined");
        }
        if (columns == null || columns.isEmpty()) {
            throw new RuntimeException("res schema(" + currentSchema + ") need columns");
        }

        Set<String> columnCheckRepeatedSet = new HashSet<>();
        List<Object> innerList = new ArrayList<>();
        boolean hasColumnOrFunction = false;
        for (Object obj : columns) {
            if (obj != null) {
                if (obj instanceof String column) {
                    if (!column.isEmpty()) {
                        QueryUtil.checkColumnName(column, currentSchema, schemaColumnInfo, "result select");
                        if (columnCheckRepeatedSet.contains(column)) {
                            throw new RuntimeException("res schema(" + currentSchema + ") column(" + column + ") has repeated");
                        }
                        columnCheckRepeatedSet.add(column);
                        hasColumnOrFunction = true;
                    }
                } else if (obj instanceof List<?> groups) {
                    if (!groups.isEmpty()) {
                        int size = groups.size();
                        if (size < 3) {
                            throw new RuntimeException("res schema(" + currentSchema + ") function(" + groups + ") error");
                        }
                        String column = QueryUtil.toStr(groups.get(1));
                        if (column.isEmpty()) {
                            throw new RuntimeException("res schema(" + currentSchema + ") function(" + groups + ") column error");
                        }
                        ReqResultGroup group = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(0)));
                        String checkType = "result schema(" + currentSchema + ") function(" + group.name().toLowerCase() + ")";
                        // noinspection EnhancedSwitchMigration
                        switch (group) {
                            case COUNT: {
                                if (!Set.of("*", "1").contains(column)) {
                                    String schemaName = QueryUtil.getSchemaName(column, currentSchema);
                                    if (!currentSchema.equals(schemaName) && !paramSchemaSet.contains(schemaName)) {
                                        throw new RuntimeException("res schema(" + currentSchema + ") function("
                                                + groups + ") no column in param");
                                    }
                                    QueryUtil.checkColumnName(column, currentSchema, schemaColumnInfo, checkType);
                                }
                                break;
                            }
                            case COUNT_DISTINCT: {
                                for (String col : column.split(",")) {
                                    String schemaName = QueryUtil.getSchemaName(col.trim(), currentSchema);
                                    if (!currentSchema.equals(schemaName) && !paramSchemaSet.contains(schemaName)) {
                                        throw new RuntimeException("res schema(" + currentSchema + ") function("
                                                + groups + ") no column in param");
                                    }
                                    QueryUtil.checkColumnName(col.trim(), currentSchema, schemaColumnInfo, checkType);
                                }
                                break;
                            }
                            default: {
                                String schemaName = QueryUtil.getSchemaName(column, currentSchema);
                                if (!currentSchema.equals(schemaName) && !paramSchemaSet.contains(schemaName)) {
                                    throw new RuntimeException("res schema(" + currentSchema + ") function("
                                            + groups + ") no column in param");
                                }
                                QueryUtil.checkColumnName(column, currentSchema, schemaColumnInfo, checkType);
                                break;
                            }
                        }
                        if (size > 4) {
                            // 先右移 1 位除以 2, 再左移 1 位乘以 2, 变成偶数
                            int evenSize = size >> 1 << 1;
                            for (int i = 3; i < evenSize; i += 2) {
                                ReqParamConditionType conditionType = ReqParamConditionType.deserializer(groups.get(i));
                                if (conditionType == null) {
                                    throw new RuntimeException("res schema(" + currentSchema + ") function("
                                            + groups + ") having condition error");
                                }

                                Object value = groups.get(i + 1);
                                if (group.checkHavingValue(value)) {
                                    throw new RuntimeException("res schema(" + currentSchema + ") function("
                                            + groups + ") having condition value(" + value + ") type error");
                                }
                            }
                        }
                        hasColumnOrFunction = true;
                    }
                } else {
                    innerList.add(obj);
                }
            }
        }
        if (!hasColumnOrFunction) {
            throw new RuntimeException("res schema(" + currentSchema + ") no columns");
        }

        for (Object obj : innerList) {
            Map<String, ReqResult> inner = JsonUtil.convertType(obj, QueryConst.RESULT_TYPE);
            if (inner == null) {
                throw new RuntimeException("res schema(" + currentSchema + ") relation(" + obj + ") error");
            }
            for (Map.Entry<String, ReqResult> entry : inner.entrySet()) {
                String column = entry.getKey();
                ReqResult innerResult = entry.getValue();
                if (innerResult == null) {
                    throw new RuntimeException("res schema(" + currentSchema + ") relation column(" + column + ") error");
                }
                if (columnCheckRepeatedSet.contains(column)) {
                    throw new RuntimeException("res schema(" + currentSchema + ") relation column(" + column + ") has repeated");
                }
                columnCheckRepeatedSet.add(column);
                String innerSchema = innerResult.getSchema();
                if (innerSchema == null || innerSchema.isEmpty()) {
                    throw new RuntimeException("res schema(" + currentSchema + ") inner(" + column + ") need schema");
                }
                if (schemaColumnInfo.findRelationByMasterChild(currentSchema, innerSchema) == null) {
                    throw new RuntimeException("res " + currentSchema + " - " + column + " -" + innerSchema + " has no relation");
                }
                innerResult.checkResult(innerSchema, schemaColumnInfo, paramSchemaSet);
            }
        }
    }

    public String generateSelectSql(String mainSchema, boolean needAlias, Set<String> paramSchemaSet,
                                    SchemaColumnInfo schemaColumnInfo) {
        String currentSchemaName = (schema == null || schema.trim().isEmpty()) ? mainSchema : schema.trim();
        Schema schema = schemaColumnInfo.findSchema(currentSchemaName);
        StringJoiner sj = new StringJoiner(", ");
        Set<String> columnNameSet = new HashSet<>();

        for (Object obj : columns) {
            if (obj instanceof String column) {
                if (!column.isEmpty()) {
                    String schemaName = QueryUtil.getSchemaName(column, currentSchemaName);
                    if (schemaName.equals(currentSchemaName) || paramSchemaSet.contains(schemaName)) {
                        Schema currentSchema = schemaColumnInfo.findSchema(schemaName);
                        String columnName = QueryUtil.getColumnName(column);
                        SchemaColumn schemaColumn = schemaColumnInfo.findSchemaColumn(currentSchema, columnName);

                        String addKey = currentSchema.getName() + "." + schemaColumn.getName();
                        if (!columnNameSet.contains(addKey)) {
                            String schemaColumnName = QuerySqlUtil.toSqlField(schemaColumn.getName());
                            if (needAlias) {
                                sj.add(QuerySqlUtil.toSqlField(currentSchema.getAlias()) + "." + schemaColumnName);
                            } else {
                                sj.add(schemaColumnName);
                            }
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
                        String schemaColumnName = QuerySqlUtil.toSqlField(relation.getOneColumn());
                        if (needAlias) {
                            sj.add(QuerySqlUtil.toSqlField(schema.getAlias()) + "." + schemaColumnName);
                        } else {
                            sj.add(schemaColumnName);
                        }
                    }
                    columnNameSet.add(addKey);
                }
            }
        }
        return sj.toString();
    }

    public String generateFunctionSql(String mainSchema, boolean needAlias, SchemaColumnInfo schemaColumnInfo,
                                      Map<String, String> functionAliasMap) {
        StringJoiner sj = new StringJoiner(", ");
        for (Object obj : columns) {
            if (obj instanceof List<?> groups) {
                if (!groups.isEmpty()) {
                    String column = QueryUtil.toStr(groups.get(1));
                    if (!column.isEmpty()) {
                        ReqResultGroup group = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(0)));
                        String useName = QueryUtil.getRealColumn(needAlias, column, mainSchema, schemaColumnInfo);
                        String alias = group.generateAlias(column);
                        sj.add(group.generateColumn(useName) + " AS " + alias);
                        functionAliasMap.put(alias, QueryUtil.toStr(groups.get(2)));
                    }
                }
            }
        }
        return sj.toString();
    }

    public String generateGroupSql(String mainSchema, boolean needAlias, SchemaColumnInfo schemaColumnInfo) {
        StringJoiner sj = new StringJoiner(", ");
        boolean hasGroup = false;
        for (Object obj : columns) {
            if (obj instanceof String column) {
                if (!column.isEmpty()) {
                    sj.add(QueryUtil.getRealColumn(needAlias, column, mainSchema, schemaColumnInfo));
                }
            } else if (obj instanceof List<?> groups) {
                if (!groups.isEmpty()) {
                    hasGroup = true;
                }
            }
        }
        if (!hasGroup) {
            return "";
        }
        String groupBy = sj.toString();
        return groupBy.isEmpty() ? "" : (" GROUP BY " + groupBy);
    }

    public String generateHavingSql(List<Object> params) {
        StringJoiner groupSj = new StringJoiner(" AND ");
        for (Object obj : columns) {
            if (obj instanceof List<?> groups) {
                if (!groups.isEmpty()) {
                    int size = groups.size();
                    if (size > 4) {
                        String havingField = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(0))).havingField(groups);
                        // 先右移 1 位除以 2, 再左移 1 位乘以 2, 变成偶数
                        int evenSize = size >> 1 << 1;
                        for (int i = 3; i < evenSize; i += 2) {
                            ReqParamConditionType conditionType = ReqParamConditionType.deserializer(groups.get(i));
                            Object value = groups.get(i + 1);

                            groupSj.add(conditionType.generateSql(havingField, value, params));
                        }
                    }
                }
            }
        }
        String groupBy = groupSj.toString();
        return groupBy.isEmpty() ? "" : (" HAVING " + groupBy);
    }
}
