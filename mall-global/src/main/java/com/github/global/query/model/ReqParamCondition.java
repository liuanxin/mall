package com.github.global.query.model;

import com.github.global.query.enums.ReqParamConditionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReqParamCondition {

    private String column;
    private ReqParamConditionType type;
    private Object value;
}
