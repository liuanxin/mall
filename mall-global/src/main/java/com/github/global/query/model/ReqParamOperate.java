package com.github.global.query.model;

import com.github.global.query.enums.ParamConditionType;
import com.github.global.query.enums.ParamOperateType;
import com.github.global.query.util.QueryJsonUtil;
import com.github.global.query.util.QueryUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * <pre>
 * name like 'abc%'   and gender = 1   and age between 18 and 40
 * and province in ( 'x', 'y', 'z' )   and city like '%xx%'   and time >= now()
 * {
 *   -- "schema": "order",   -- 不设置则从 requestInfo 中获取
 *   -- "operate": "and",    -- 并且(and) 和 或者(or) 两种, 不设置则默认是 and
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
    /** 条件拼接类型: 并且(and) 和 或者(or) 两种, 不设置则默认是 and */
    private ParamOperateType operate;
    /** 条件 */
    private List<Object> conditions;


    public Set<String> checkCondition(String mainSchema, SchemaColumnInfo scInfo) {
        if (conditions == null || conditions.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> querySchemaSet = new LinkedHashSet<>();
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

                    Schema sa = scInfo.findSchema(QueryUtil.getSchemaName(column, currentSchema));
                    if (sa == null) {
                        throw new RuntimeException("param condition(" + condition + ") column has no schema info");
                    }
                    querySchemaSet.add(sa.getName());

                    boolean standardSize = (size == 2);
                    ParamConditionType type = standardSize ? ParamConditionType.EQ : ParamConditionType.deserializer(list.get(1));
                    if (type == null) {
                        throw new RuntimeException(String.format("param condition column(%s) need type", column));
                    }

                    SchemaColumn schemaColumn = scInfo.findSchemaColumn(sa, QueryUtil.getColumnName(column));
                    if (schemaColumn == null) {
                        throw new RuntimeException(String.format("param condition column(%s) has no column info", column));
                    }
                    type.checkTypeAndValue(schemaColumn.getColumnType(), column, list.get(standardSize ? 1 : 2), schemaColumn.getStrLen());
                } else {
                    ReqParamOperate compose = QueryJsonUtil.convert(condition, ReqParamOperate.class);
                    if (compose == null) {
                        throw new RuntimeException("compose condition(" + condition + ") error");
                    }
                    compose.checkCondition(currentSchema, scInfo);
                }
            }
        }
        return querySchemaSet;
    }

    public String generateSql(String mainSchema, SchemaColumnInfo scInfo, boolean needAlias, List<Object> params) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }

        String operateType = (operate == null ? ParamOperateType.AND : operate).name().toUpperCase();
        StringJoiner sj = new StringJoiner(" " + operateType + " ");
        String currentSchema = (schema == null || schema.trim().isEmpty()) ? mainSchema : schema.trim();
        for (Object condition : conditions) {
            if (condition != null) {
                if (condition instanceof List<?> list) {
                    if (!list.isEmpty()) {
                        int size = list.size();
                        String column = QueryUtil.toStr(list.get(0));

                        boolean standardSize = (size == 2);
                        ParamConditionType type = standardSize ? ParamConditionType.EQ : ParamConditionType.deserializer(list.get(1));
                        Object value = list.get(standardSize ? 1 : 2);

                        String schemaName = QueryUtil.getSchemaName(column, currentSchema);
                        String columnName = QueryUtil.getColumnName(column);
                        Class<?> columnType = scInfo.findSchemaColumn(schemaName, columnName).getColumnType();
                        String useColumn = QueryUtil.getUseColumn(needAlias, column, currentSchema, scInfo);
                        String sql = type.generateSql(useColumn, columnType, value, params);
                        if (!sql.isEmpty()) {
                            sj.add(sql);
                        }
                    }
                } else {
                    ReqParamOperate compose = QueryJsonUtil.convert(condition, ReqParamOperate.class);
                    if (compose != null) {
                        String innerWhereSql = compose.generateSql(currentSchema, scInfo, needAlias, params);
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
