package com.github.global.query.model;

import com.github.common.json.JsonUtil;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.util.MysqlKeyWordUtil;
import com.github.global.query.util.QueryUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * <pre>
 * SELECT id, orderNo FROM t_order ...
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
 *   COUNT(*) AS cnt,
 *   COUNT(name) AS cnt_name,   -- 别名是自动拼装的
 *   SUM(price) AS sum_price,
 *   MIN(id) AS min_id,
 *   MAX(id) AS max_id,
 *   AVG(price) AS avg_price,
 *   GROUP_CONCAT(name) AS gct_name
 * from ...
 * GROUP BY name
 * {
 *   "columns": [
 *     "name",
 *     [ "count", "*", "x" ],    -- 第三个参数表示接口响应回去时的属性
 *     [ "count", "name", "xx" ],
 *     [ "sum", "price", "xxx" ],
 *     [ "min", "id", "y" ],
 *     [ "max", "id", "yy" ],
 *     [ "avg", "price", "yyy" ],
 *     [ "gct", "name", "z" ]
 *   ]
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReqResult {

    /** 结构 */
    private String schema;
    /** 结构类型, 对象(obj)还是数组(arr) */
    private ReqResultType type;
    /** 结构里的列 */
    private List<Object> columns;


    public void checkResult(String mainSchema, SchemaColumnInfo columnInfo) {
        String currentSchema = (schema == null || schema.trim().isEmpty()) ? mainSchema : schema.trim();
        if (currentSchema == null || currentSchema.isEmpty()) {
            throw new RuntimeException("res need schema");
        }

        Map<String, Schema> schemaMap = columnInfo.getSchemaMap();
        if (!schemaMap.containsKey(currentSchema)) {
            throw new RuntimeException("no schema(" + currentSchema + ") defined");
        }

        if (columns != null && !columns.isEmpty()) {
            StringJoiner sj = new StringJoiner(", ");
            Set<String> columnCheckRepeatedSet = new HashSet<>();
            List<Object> innerList = new ArrayList<>();
            for (Object obj : columns) {
                if (obj != null) {
                    if (obj instanceof String column) {
                        if (!column.isEmpty()) {
                            QueryUtil.checkSchemaAndColumnName(currentSchema, column, columnInfo, "result select");
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
                                    QueryUtil.checkColumnName(column, currentSchema, columnInfo, checkType);
                                }
                            } else {
                                QueryUtil.checkColumnName(column, currentSchema, columnInfo, checkType);
                            }
                        }
                    } else {
                        innerList.add(obj);
                    }
                }
            }

            Map<String, SchemaColumnRelation> relationMap = columnInfo.getRelationMap();
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
                    innerResult.checkResult(currentSchema, columnInfo);
                }
            }
        }
    }

    public Set<String> allResultSchema(String mainSchema, SchemaColumnInfo schemaColumnInfo) {
        Set<String> set = new LinkedHashSet<>();
        String currentSchema = (schema == null || schema.trim().isEmpty()) ? mainSchema : schema.trim();
        if (columns != null && !columns.isEmpty()) {
            StringJoiner sj = new StringJoiner(", ");
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
                                set.addAll(innerResult.allResultSchema(currentSchema, schemaColumnInfo));
                            }
                        }
                    }
                }
            }
        }
        return set;
    }

    public String generateSelectSql(String mainSchema, SchemaColumnInfo columnInfo) {
        if (columns == null || columns.isEmpty()) {
            return "";
        }

        StringJoiner sj = new StringJoiner(", ");
        for (Object obj : columns) {
            if (obj instanceof String column) {
                if (!column.isEmpty()) {
                    sj.add(QueryUtil.checkSchemaAndColumnName(mainSchema, column, columnInfo, "result select").getName());
                }
//                } else { // todo
            }
        }
        return sj.toString();
    }

//    public String generateFunctionSql(String mainSchema, TableColumnInfo columnInfo) {
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
//                            QueryUtil.checkColumnName(column, mainSchema, columnInfo, checkType);
//                        }
//                    } else {
//                        QueryUtil.checkColumnName(column, mainSchema, columnInfo, checkType);
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

    public String generateGroupSql() {
        StringJoiner groupSj = new StringJoiner(", ");
        for (Object obj : columns) {
            if (obj instanceof String column && !column.isEmpty()) {
                groupSj.add(MysqlKeyWordUtil.toSql(column));
            }
        }
        String groupBy = groupSj.toString();
        return groupBy.isEmpty() ? "" : (" GROUP BY " + groupBy);
    }
}
