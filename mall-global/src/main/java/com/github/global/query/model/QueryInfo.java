package com.github.global.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    public static class QueryColumn {

        /** 表列名 */
        private String name;

        /** 表列说明 */
        private String desc;

        /** 表列对应的实体的类型 */
        private String type;
    }
}
