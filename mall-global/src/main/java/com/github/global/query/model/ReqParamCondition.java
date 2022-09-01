package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqParamCondition {

    private String column;
    private ReqParamConditionType type;
    private Object value;

    /** 用在 =、LIKE(包含, 开头, 结尾)、>、>=、<、<= 时 */
    public ReqParamCondition(String column, ReqParamConditionType type, Object value) {
        this.column = column;
        this.type = type;
        this.value = value;
    }
    /** 用在 IN、NOT IN 时 */
    public ReqParamCondition(String column, boolean notIn, Object values) {
        this.column = column;
        this.type = notIn ? ReqParamConditionType.NOT_IN : ReqParamConditionType.IN;
        this.value = values;
    }
    /** 在时间或数字上用 BETWEEN 时 */
    public ReqParamCondition(String column, Object range) {
        this.column = column;
        this.type = ReqParamConditionType.BETWEEN;
        this.value = range;
    }
}
