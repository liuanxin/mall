package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqParam {

    private List<ReqParamCondition<?>> conditions;

    /** { "create_time": "desc", "id", "asc" } */
    private Map<String, String> orders;

    private Integer page;
    private Integer limit;
}
