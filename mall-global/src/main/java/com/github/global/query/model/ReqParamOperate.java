package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.global.query.util.QueryUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * name like 'abc%'   and gender = 1   and age between 18 and 40   and province in ( 'x', 'y', 'z' )   and city like '%xx%'   and time >= now()
 * {
 *   -- "operate": "and", -- 如果是 and 可以省略
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
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ],
 *   "composes": [  -- 需要组合, 且与上面的条件不是同一个类型的条件就用这种方式
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
 *     }
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
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ],
 *   "composes": [
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

    /** 条件拼接类型: and 还是 or */
    private ReqParamOperateType operate;
    /** 条件 */
    private List<List<Object>> conditions;

    /** 组合的条件 */
    private List<ReqParamOperate> composes;


    public List<ReqParamCondition> checkCondition(String defaultScheme, Map<String, Scheme> schemeMap) {
        if (conditions == null || conditions.isEmpty()) {
            return Collections.emptyList();
        }

        List<ReqParamCondition> conditionList = new ArrayList<>();
        for (List<Object> condition : conditions) {
            if (condition != null && !condition.isEmpty()) {
                int size = condition.size();
                if (size < 2) {
                    throw new RuntimeException("条件构建有误");
                }

                String column = toStr(condition.get(0));
                if (column.isEmpty()) {
                    return Collections.emptyList();
                }
                ReqParamConditionType type;
                Object value;
                if (size == 2) {
                    type = ReqParamConditionType.EQ;
                    value = condition.get(1);
                } else {
                    type = ReqParamConditionType.deserializer(condition.get(1));
                    value = condition.get(2);
                }
                if (type  == null) {
                    throw new RuntimeException(String.format("列(%s)条件有误", column));
                }

                // 用传入的列名获取其结构上定义的类型
                Class<?> columnType = QueryUtil.checkColumnName(column, defaultScheme, schemeMap, "query").getColumnType();
                // 检查列类型和传入的值是否对应
                type.checkTypeAndValue(columnType, column, value);
                conditionList.add(new ReqParamCondition(column, type, value));
            }
        }
        return conditionList;
    }

    private static String toStr(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }
}
