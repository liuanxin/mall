package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqParam {

    private List<ReqParamCondition> conditions;

    private List<String> groups;

    private List<ReqParamOrder> orders;

    private ReqParamPage page;
}
