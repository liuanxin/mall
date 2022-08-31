package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqParamCondition<T> {

    private String column;
    private ReqParamConditionType type = ReqParamConditionType.EQ;
    private T value;
    private T start;
    private T end;

    public ReqParamCondition(String column, T value) {
        this.column = column;
        this.value = value;
    }

    public ReqParamCondition(String column, T start, T end) {
        this.column = column;
        this.start = start;
        this.end = end;
    }
}
