package com.github.global.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaColumn {

    /** 表列名 */
    private String name;

    /** 表列说明 */
    private String desc;

    /** 表列别名 */
    private String alias;

    /** true 表示是主键字段 */
    private boolean primary;

    /** 表列对应的实体的类型 */
    private Class<?> columnType;
}
