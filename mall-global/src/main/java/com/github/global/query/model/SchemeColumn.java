package com.github.global.query.model;

import lombok.Data;

@Data
public class SchemeColumn {

    /** 表列名 */
    private String name;

    /** 表列名说明 */
    private String desc;

    /** 表列名别名 */
    private String alias;

    /** 表列对应的实体的类型 */
    private Class<?> type;
}
