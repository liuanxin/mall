package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.common.json.JsonUtil;
import com.github.global.query.util.QueryUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * name like 'abc%'   and gender = 1   and age between 18 and 40   and province in ( 'x', 'y', 'z' )   and city like '%xx%'   and time >= now()
 * {
 *   -- "scheme": "order",   -- 忽略将从 requestInfo 中获取
 *   -- "operate": "and",    -- 如果是 and 可以省略
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
 * name like 'abc%'   and ( gender = 1 or age between 18 and 40 )   and ( province in ( 'x', 'y', 'z' ) or city like '%xx%' )   and time >= now()
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
 * name like 'abc%'   or gender = 1   or age between 18 and 40   or province in ( 'x', 'y', 'z' )   or city like '%xx%'   or time >= now()
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
 * name like 'abc%'   or time >= now()   or ( gender = 1 and age between 18 and 40 )   or ( province in ( 'x', 'y', 'z' ) and city like '%xx%' )
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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqParamOperate {

    private String scheme;
    /** 条件拼接类型: and 还是 or */
    private ReqParamOperateType operate;
    /** 条件 */
    private List<Object> conditions;


    public void checkCondition(String mainScheme, TableColumnInfo columnInfo) {
        if (conditions == null || conditions.isEmpty()) {
            return;
        }

        String currentScheme = (scheme == null || scheme.trim().isEmpty()) ? mainScheme : scheme.trim();
        if (currentScheme == null || currentScheme.isEmpty()) {
            throw new RuntimeException("param need scheme");
        }

        List<ReqParamCondition> conditionList = new ArrayList<>();
        for (Object condition : conditions) {
            if (condition != null) {
                if (condition instanceof List<?> list) {
                    if (!list.isEmpty()) {
                        int size = list.size();
                        if (size < 2) {
                            throw new RuntimeException("condition(" + condition + ") error");
                        }

                        String column = QueryUtil.toStr(list.get(0));
                        if (!column.isEmpty()) {
                            ReqParamConditionType type;
                            Object value;
                            if (size == 2) {
                                type = ReqParamConditionType.EQ;
                                value = list.get(1);
                            } else {
                                type = ReqParamConditionType.deserializer(list.get(1));
                                value = list.get(2);
                            }
                            if (type == null) {
                                throw new RuntimeException(String.format("condition column(%s) need type", column));
                            }

                            // 用传入的列名获取其结构上定义的类型
                            SchemeColumn schemeColumn = QueryUtil.checkColumnName(column, currentScheme,
                                    columnInfo, "query condition");
                            // 检查列类型和传入的值是否对应
                            type.checkTypeAndValue(schemeColumn.getColumnType(), column, value);
                        }
                    }
                } else {
                    ReqParamOperate compose = JsonUtil.convert(condition, ReqParamOperate.class);
                    if (compose == null) {
                        throw new RuntimeException("compose condition(" + condition + ") error");
                    }
                    compose.checkCondition(currentScheme, columnInfo);
                }
            }
        }
    }
}
