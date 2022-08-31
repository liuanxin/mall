package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqParamCondition<T> {

    private String column;
    private ReqParamConditionType type;
    private T value;
    private T start;
    private T end;

    /** 用在 =、IN(value 需要是 List)、LIKE、>、>=、<、<=、 时 */
    public ReqParamCondition(String column, ReqParamConditionType type, T value) {
        this.column = column;
        this.type = type;
        this.value = value;
    }

    /** 用在 >= AND <= 等时间或数字类型时 */
    public ReqParamCondition(String column, T start, T end) {
        this.column = column;
        this.type = ReqParamConditionType.BETWEEN;
        this.start = start;
        this.end = end;
    }
}
