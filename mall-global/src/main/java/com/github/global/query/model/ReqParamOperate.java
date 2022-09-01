package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * <pre>
 * name like 'abc%'   and gender = 1   and age between 18 and 40   and time >= now()
 * {
 *   "conditions": [
 *     { "column": "name", "type": "rl", "value": "abc" },
 *     { "column": "gender", "type": "eq", "value": 1 },
 *     { "column": "age", "type": "between", "start": 18, "end": 40 },
 *     { "column": "time", "type": "ge", "value": "xxxx-xx-xx xx:xx:xx" },
 *   ]
 * }
 *
 *
 * name like 'abc%'   and time >= now()   and ( gender = 1 or age between 18 and 40 )   and ( province in ( 'x', 'y', 'z' ) or city like '%xx%' )
 * {
 *   "conditions": [
 *     { "column": "name", "type": "rl", "value": "abc" },
 *     { "column": "time", "type": "ge", "value": "xxxx-xx-xx xx:xx:xx" },
 *   ],
 *   "composes": [
 *     {
 *       "operate": "or",
 *       "conditions": [
 *         { "column": "gender", "type": "eq", "value": 1 },
 *         { "column": "age", "type": "between", "start": 18, "end": 40 }
 *       ]
 *     },
 *     {
 *       "operate": "or",
 *       "conditions": [
 *         { "column": "province", "type": "in", "value": [ 'x', 'y', 'z' ] },
 *         { "column": "city", "type": "like", "value": "xx" }
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
 *     { "column": "name", "type": "rl", "value": "abc" },
 *     { "column": "gender", "type": "eq", "value": 1 },
 *     { "column": "age", "type": "between", "start": 18, "end": 40 },
 *     { "column": "time", "type": "ge", "value": "xxxx-xx-xx xx:xx:xx" },
 *   ]
 * }
 *
 *
 * name like 'abc%'   or time >= now()   or ( gender = 1 and age between 18 and 40 )   or ( province in ( 'x', 'y', 'z' ) and city like '%xx%' )
 * {
 *   "operate": "or",
 *   "conditions": [
 *     { "column": "name", "type": "rl", "value": "abc" },
 *     { "column": "time", "type": "ge", "value": "xxxx-xx-xx xx:xx:xx" },
 *   ],
 *   "composes": [
 *     {
 *       "conditions": [
 *         { "column": "gender", "type": "eq", "value": 1 },
 *         { "column": "age", "type": "between", "start": 18, "end": 40 }
 *       ]
 *     },
 *     {
 *       "conditions": [
 *         { "column": "province", "type": "in", "value": [ 'x', 'y', 'z' ] },
 *         { "column": "city", "type": "like", "value": "xx" }
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
    private List<ReqParamCondition<?>> conditions;

    private List<ReqParamOperate> composes;
}
