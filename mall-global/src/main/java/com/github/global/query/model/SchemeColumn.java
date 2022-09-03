package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SchemeColumn {

    /** 表列名 */
    private String name;

    /** 表列说明 */
    private String desc;

    /** 表列别名 */
    private String alias;

    /** 表列对应的实体的类型 */
    @JsonIgnore
    private Class<?> columnType;
}
