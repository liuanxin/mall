package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqParamCondition {

    private String field;
    /*
    global:
        is null
        is not null
        = (等于)
        <>

    list:
        in (批量)
        not in

    number/date:
        >
        >=
        <
        <=
        between

    string:
        like (开头、结尾、包含), 只有「开头」会走索引(LIKE 'x%'), 结尾是 LIKE '%xx', 包含是 LIKE '%xxx%'
        not like
    */
    private String type = "eq";
    private String value;
    private String start;
    private String end;
}
