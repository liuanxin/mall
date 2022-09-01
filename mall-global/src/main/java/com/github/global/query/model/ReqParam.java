package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqParam {

    private ReqParamOperate query;

    /** { "create_time": "desc", "id", "asc" } */
    private Map<String, String> order;

    private Integer page;

    private Integer limit;
}
