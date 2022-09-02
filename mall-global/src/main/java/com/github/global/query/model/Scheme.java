package com.github.global.query.model;

import lombok.Data;

import java.util.Map;

@Data
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
