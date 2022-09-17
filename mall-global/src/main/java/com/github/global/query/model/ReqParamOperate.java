package com.github.global.query.model;

import com.github.common.json.JsonUtil;
import com.github.global.query.enums.ReqParamConditionType;
import com.github.global.query.enums.ReqParamOperateType;
import com.github.global.query.util.QueryUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * <pre>
 * name like 'abc%'   and gender = 1   and age between 18 and 40
 * and province in ( 'x', 'y', 'z' )   and city like '%xx%'   and time >= now()
 * {
 *   -- "schema": "order",   -- 不设置则从 requestInfo 中获取
 *   -- "operate": "and",    -- 并且(and) 和 或者(or) 两种, 不设置则默认是 并且
 *   "conditions": [
 *     [ "name", "rl", "abc" ],
 *     [ "gender", -- "eq", --  1 ],  -- eq 可以省略
 *     [ "age", "bet", [ 18, 40 ] ],
 *     [ "province", "in", [ "x", "y", "z" ] ],
 *     [ "city", "like", "xx" ],
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ]
 * }
 *
 *
 * name like 'abc%'   and ( gender = 1 or age between 18 and 40 )
 * and ( province in ( 'x', 'y', 'z' ) or city like '%xx%' )   and time >= now()
 * {
 *   "conditions": [
 *     [ "name", "rl", "abc" ],
 *     {
 *       "operate": "or",
 *       "conditions": [
 *         [ "gender", 1 ],
 *         [ "age", "bet", [ 18, 40 ] ]
 *       ]
 *     },
 *     {
 *       "operate": "or",
 *       "conditions": [
 *         [ "province", "in", [ "x", "y", "z" ] ],
 *         [ "city", "like", "xx" ]
 *       ]
 *     },
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ]
 * }
 *
 *
 * name like 'abc%'   or gender = 1   or age between 18 and 40
 * or province in ( 'x', 'y', 'z' )   or city like '%xx%'   or time >= now()
 * {
 *   "operate": "or",
 *   "conditions": [
 *     [ "name", "rl", "abc" ],
 *     [ "gender", 1 ],
 *     [ "age", "bet", [ 18, 40 ] ],
 *     [ "province", "in", [ "x", "y", "z" ] ],
 *     [ "city", "like", "xx" ],
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ]
 * }
 *
 *
 * name like 'abc%'   or time >= now()
 * or ( gender = 1 and age between 18 and 40 )
 * or ( province in ( 'x', 'y', 'z' ) and city like '%xx%' )
 * {
 *   "operate": "or",
 *   "conditions": [
 *     [ "name", "rl", "abc" ],
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ],
 *     {
 *       "conditions": [
 *         [ "gender", 1 ],
 *         [ "age", "bet", [ 18, 40 ] ]
 *       ]
 *     },
 *     {
 *       "conditions": [
 *         [ "province", "in", [ "x", "y", "z" ] ],
 *         [ "city", "like", "xx" ]
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReqParamOperate {

    private String schema;
    /** 条件拼接类型: and 还是 or */
    private ReqParamOperateType operate;
    /** 条件 */
    private List<Object> conditions;


    public void checkCondition(String mainSchema, SchemaColumnInfo schemaColumnInfo) {
        if (conditions == null || conditions.isEmpty()) {
            return;
        }

        String currentSchema = (schema == null || schema.trim().isEmpty()) ? mainSchema : schema.trim();
        for (Object condition : conditions) {
            if (condition != null) {
                if (condition instanceof List<?> list) {
                    if (list.isEmpty()) {
                        throw new RuntimeException("param condition(" + condition + ") can't be blank");
                    }
                    int size = list.size();
                    if (size < 2) {
                        throw new RuntimeException("param condition(" + condition + ") error");
                    }
                    String column = QueryUtil.toStr(list.get(0));
                    if (column.isEmpty()) {
                        throw new RuntimeException("param condition(" + condition + ") column can't be blank");
                    }
                    boolean standardSize = (size == 2);
                    ReqParamConditionType type = standardSize ? ReqParamConditionType.EQ : ReqParamConditionType.deserializer(list.get(1));
                    if (type == null) {
                        throw new RuntimeException(String.format("condition column(%s) need type", column));
                    }
                    //检查结构名和列名
                    SchemaColumn schemaColumn = QueryUtil.checkColumnName(column, currentSchema, schemaColumnInfo, "param condition");
                    // 检查列类型和传入的值是否对应
                    type.checkTypeAndValue(schemaColumn.getColumnType(), column, list.get(standardSize ? 1 : 2), schemaColumn.getStrLen());
                } else {
                    ReqParamOperate compose = JsonUtil.convert(condition, ReqParamOperate.class);
                    if (compose == null) {
                        throw new RuntimeException("compose condition(" + condition + ") error");
                    }
                    compose.checkCondition(currentSchema, schemaColumnInfo);
                }
            }
        }
    }

    public void allSchema(String mainSchema, Set<String> set) {
        if (conditions == null || conditions.isEmpty()) {
            return;
        }

        String currentSchema = (schema == null || schema.trim().isEmpty()) ? mainSchema : schema.trim();
        set.add(currentSchema);

        for (Object condition : conditions) {
            if (condition != null) {
                if (condition instanceof List<?> list) {
                    if (!list.isEmpty()) {
                        set.add(QueryUtil.getSchemaName(QueryUtil.toStr(list.get(0)), currentSchema));
                    }
                } else {
                    ReqParamOperate compose = JsonUtil.convert(condition, ReqParamOperate.class);
                    if (compose != null) {
                        compose.allSchema(currentSchema, set);
                    }
                }
            }
        }
    }

    public String generateSql(String mainSchema, SchemaColumnInfo schemaColumnInfo, List<Object> params, boolean needAlias) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }

        StringJoiner sj = new StringJoiner(" " + operate.name().toUpperCase() + " ");
        String currentSchema = (schema == null || schema.trim().isEmpty()) ? mainSchema : schema.trim();
        for (Object condition : conditions) {
            if (condition != null) {
                if (condition instanceof List<?> list) {
                    if (!list.isEmpty()) {
                        int size = list.size();
                        String column = QueryUtil.toStr(list.get(0));

                        boolean standardSize = (size == 2);
                        ReqParamConditionType type = standardSize ? ReqParamConditionType.EQ : ReqParamConditionType.deserializer(list.get(1));
                        Object value = list.get(standardSize ? 1 : 2);

                        String realColumn = QueryUtil.getRealColumn(needAlias, column, currentSchema, schemaColumnInfo);
                        String sql = type.generateSql(realColumn, value, params);
                        if (!sql.isEmpty()) {
                            sj.add(sql);
                        }
                    }
                } else {
                    ReqParamOperate compose = JsonUtil.convert(condition, ReqParamOperate.class);
                    if (compose != null) {
                        String innerWhereSql = compose.generateSql(currentSchema, schemaColumnInfo, params, needAlias);
                        if (!innerWhereSql.isEmpty()) {
                            sj.add("( " + innerWhereSql + " )");
                        }
                    }
                }
            }
        }
        return sj.toString().trim();
    }
}
