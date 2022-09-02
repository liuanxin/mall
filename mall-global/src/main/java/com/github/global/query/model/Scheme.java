package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Scheme {

    /** 表名 */
    private String name;

    /** 表说明 */
    private String desc;

    /** 表别名 */
    private String alias;

    /** 列信息 */
    private Map<String, SchemeColumn> columnMap;
}
