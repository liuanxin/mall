package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <pre>
 * name like 'abc%'   and gender = 1   and age between 18 and 40   and time >= now()
 * {
 *   "conditions": [
 *     [ "name", "rl", "abc" ],
 *     [ "gender", "eq", 1 ],
 *     [ "age", "between", [ 18, 40 ] ],
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ]
 * }
 *
 *
 * name like 'abc%'   and time >= now()   and ( gender = 1 or age between 18 and 40 )   and ( province in ( 'x', 'y', 'z' ) or city like '%xx%' )
 * {
 *   "conditions": [
 *     [ "name", "rl", "abc" ],
 *     [ "time", "ge", "xxxx-xx-xx xx:xx:xx" ]
 *   ],
 *   "composes": [
 *     {
 *       "operate": "or",
 *       "conditions": [
 *         [ "gender", "eq", 1 ],
 *         [ "age", "between", [ 18, 40 ] ]
 *       ]
 *     },
 *     {
 *       "operate": "or",
 *       "conditions": [
 *         [ "province", "in",  [ "x", "y", "z" ] ],
 *         [ "city", "like", "xx" ]
 *       ]
 *     }
 *   ]
 * }
 *
 *
 * name like 'abc%'   or gender = 1   or age between 18 and 40   or time >= now()
 * {
 *   "operate": "or",
 *   "conditions": [
 *     [ "name", "rl", "abc" ],
 *     [ "gender", "eq", 1 ],
 *     [ "age", "between", [ 18, 40 ] ],
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
 *         [ "gender", "eq", 1 ],
 *         [ "age", "between", [ 18, 40 ] ]
 *       ]
 *     },
 *     {
 *       "conditions": [
 *         [ "province", "in",  [ "x", "y", "z" ] ],
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

    private ReqParamOperateType operate;
    private List<List<Object>> conditions;

    private List<ReqParamOperate> composes;

    public List<List<ReqParamCondition>> handleCondition() {
        if (conditions == null || conditions.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<ReqParamCondition>> conditionList = new ArrayList<>();
        for (List<Object> condition : conditions) {
            if (condition != null && !condition.isEmpty()) {
                if (condition.size() != 3) {
                    throw new RuntimeException("条件构建有误");
                }

                ReqParamConditionType type = ReqParamConditionType.deserializer(condition.get(1));
                String column = toStr(condition.get(0));
                Object value = condition.get(2);
            }
        }
        return conditionList;
    }
    private static String toStr(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }
}
