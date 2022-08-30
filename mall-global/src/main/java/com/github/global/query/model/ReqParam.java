package com.github.global.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReqParam {

    private List<ReqParamCondition> conditions;

    private List<String> groups;

    private List<ReqParamOrder> orders;

    private ReqParamPage page;
}
