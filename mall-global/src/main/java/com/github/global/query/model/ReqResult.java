package com.github.global.query.model;

import com.github.common.json.JsonUtil;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.enums.ReqParamConditionType;
import com.github.global.query.enums.ReqResultGroup;
import com.github.global.query.enums.ReqResultType;
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
 *   -- "schema": "order", -- 不设置则从 requestInfo 中获取
 *   "type": "obj",        -- 对象(obj) 和 数组(arr) 两种, 不设置则默认是数组
 *   "columns": [
 *     "id",
 *     "orderNo",
 *     {
 *       "address": {
 *         "type": "obj",
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
 *   COUNT(*) AS `_cnt`,
 *   COUNT(name) AS _cnt_name,   -- 别名是自动拼装的
 *   SUM(price) AS _sum_price,
 *   MIN(id) AS _min_id,
 *   MAX(id) AS _max_id,
 *   AVG(price) AS _avg_price,
 *   GROUP_CONCAT(name) AS _gct_name
 * FROM ...
 * GROUP BY name
 * HAVING _sum_price > 100.5 AND _sum_price < 120.5
 * {
 *   "columns": [
 *     "name",
 *     [ "count", "*", "x" ],
 *     [ "count", "name", "xx" ],
 *     [ "sum", "price", "xxx", "gt", 100.5, "lt", 120.5 ],
 *     [ "min", "id", "y" ],
 *     [ "max", "id", "yy" ],
 *     [ "avg", "price", "yyy" ],
 *     [ "gct", "name", "z" ]
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
    /** 结构类型, 对象(obj)还是数组(arr), 不设置则默认是数组 */
    private ReqResultType type;
    /** 结构里的列 */
    private List<Object> columns;


    public void checkResult(String mainSchema, SchemaColumnInfo schemaColumnInfo) {
        String currentSchema = (schema == null || schema.trim().isEmpty()) ? mainSchema : schema.trim();
        if (currentSchema == null || currentSchema.isEmpty()) {
            throw new RuntimeException("res need schema");
        }
        Map<String, Schema> schemaMap = schemaColumnInfo.getSchemaMap();
        if (!schemaMap.containsKey(currentSchema)) {
            throw new RuntimeException("no res schema(" + currentSchema + ") defined");
        }
        if (columns == null || columns.isEmpty()) {
            throw new RuntimeException("res need columns");
        }

        Set<String> columnCheckRepeatedSet = new HashSet<>();
        List<Object> innerList = new ArrayList<>();
        for (Object obj : columns) {
            if (obj != null) {
                if (obj instanceof String column) {
                    if (!column.isEmpty()) {
                        QueryUtil.checkSchemaAndColumnName(currentSchema, column, schemaColumnInfo, "result select");
                        if (columnCheckRepeatedSet.contains(column)) {
                            throw new RuntimeException("res column(" + column + ") has repeated");
                        }
                        columnCheckRepeatedSet.add(column);
                    }
                } else if (obj instanceof List<?> groups) {
                    if (!groups.isEmpty()) {
                        int size = groups.size();
                        if (size < 3) {
                            throw new RuntimeException("res function(" + groups + ") error");
                        }
                        ReqResultGroup group = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(0)));
                        String column = QueryUtil.toStr(groups.get(1));
                        String checkType = "result function(" + group.name().toLowerCase() + ")";
                        if (group == ReqResultGroup.COUNT) {
                            if (!Set.of("*", "1", "0").contains(column)) {
                                QueryUtil.checkColumnName(column, currentSchema, schemaColumnInfo, checkType);
                            }
                        } else {
                            QueryUtil.checkColumnName(column, currentSchema, schemaColumnInfo, checkType);
                        }
                        if (size > 4) {
                            // 先右移 1 位除以 2, 再左移 1 位乘以 2, 变成偶数
                            int evenSize = size >> 1 << 1;
                            for (int i = 3; i < evenSize; i += 2) {
                                ReqParamConditionType conditionType = ReqParamConditionType.deserializer(groups.get(i));
                                if (conditionType == null) {
                                    throw new RuntimeException("res function(" + groups + ") having condition error");
                                }

                                Object value = groups.get(i + 1);
                                if (group.checkHavingValue(value)) {
                                    throw new RuntimeException("res function(%s"
                                            + groups + ") having condition value(" + value + ") type error");
                                }
                            }
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
                if (columnCheckRepeatedSet.contains(column)) {
                    throw new RuntimeException("res relation column(" + column + ") has repeated");
                }
                ReqResult innerResult = entry.getValue();
                if (innerResult == null) {
                    throw new RuntimeException("res relation column(" + column + ") error");
                }
                columnCheckRepeatedSet.add(column);
                innerResult.checkResult(currentSchema, schemaColumnInfo);
            }
        }
    }

    public Set<String> allResultSchema(String mainSchema) {
        Set<String> set = new LinkedHashSet<>();
        String currentSchema = (schema == null || schema.trim().isEmpty()) ? mainSchema : schema.trim();

        for (Object obj : columns) {
            if (obj != null) {
                if (obj instanceof String column) {
                    set.add(QueryUtil.getSchemaName(column, mainSchema));
                } else if (obj instanceof List<?> groups) {
                    if (!groups.isEmpty()) {
                        set.add(QueryUtil.getSchemaName(QueryUtil.toStr(groups.get(1)), mainSchema));
                    }
                } else {
                    Map<String, ReqResult> inner = JsonUtil.convertType(obj, QueryConst.RESULT_TYPE);
                    if (inner != null) {
                        for (ReqResult innerResult : inner.values()) {
                            set.addAll(innerResult.allResultSchema(currentSchema));
                        }
                    }
                }
            }
        }
        return set;
    }

    public String generateSelectSql(String mainSchema, Set<String> paramSchema, SchemaColumnInfo schemaColumnInfo) {
        // todo
        StringJoiner sj = new StringJoiner(", ");
        for (Object obj : columns) {
            if (obj instanceof String column) {
                if (!column.isEmpty()) {
                    sj.add(QueryUtil.checkSchemaAndColumnName(mainSchema, column, schemaColumnInfo, "result select").getName());
                }
            }
        }
        return sj.toString();
    }

    public String generateInnerSelectSql() {
        // todo
        return "";
    }

    public String generateSelectFunctionSql() {
        StringJoiner groupSj = new StringJoiner(", ");
        for (Object obj : columns) {
            if (obj instanceof List<?> groups) {
                if (!groups.isEmpty()) {
                    int size = groups.size();
                    if (size > 2) {
                        ReqResultGroup group = ReqResultGroup.deserializer(QueryUtil.toStr(groups.get(0)));
                        String selectFunction = group.generateSelectFunction(groups);
                        if (!selectFunction.isEmpty()) {
                            groupSj.add(selectFunction);
                        }
                    }
                }
            }
        }
        return groupSj.toString();
    }

    public String generateGroupSql() {
        StringJoiner groupSj = new StringJoiner(", ");
        boolean hasGroup = false;
        for (Object obj : columns) {
            if (obj instanceof String column && !column.isEmpty()) {
                groupSj.add(QuerySqlUtil.toSqlField(column));
            } else if (obj instanceof List<?> groups) {
                if (!groups.isEmpty()) {
                    hasGroup = true;
                }
            }
        }
        if (!hasGroup) {
            return "";
        }
        String groupBy = groupSj.toString();
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
