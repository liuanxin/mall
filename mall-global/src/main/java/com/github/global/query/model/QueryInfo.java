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
public class QueryInfo {

    /** 表使用名 */
    private String name;

    /** 表说明 */
    private String desc;

    /** 列信息 */
    private List<QueryColumn> columnList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class QueryColumn {

        /** 表列名 */
        private String name;

        /** 表列说明 */
        private String desc;

        /** 表列对应的类型 */
        private String type;

        /** 表列类型是字符串时的长度 */
        private Integer strLen;

        /** 关联关系 */
        private String relation;
    }
}
