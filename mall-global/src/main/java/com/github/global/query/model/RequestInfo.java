package com.github.global.query.model;

import com.github.global.query.enums.ResultType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestInfo {

    /** 主表 */
    private String schema;
    /** 入参 */
    private ReqParam param;
    /** 出参类型, 对象(obj)还是数组(arr), 不设置则是数组 */
    private ResultType type;
    /** 出参 */
    private ReqResult result;


    public void checkSchema(SchemaColumnInfo scInfo) {
        if (schema == null || schema.isEmpty()) {
            throw new RuntimeException("request need schema");
        }
        if (scInfo.findSchema(schema) == null) {
            throw new RuntimeException("request has no defined schema(" + schema + ")");
        }
    }

    public Set<String> checkParam(SchemaColumnInfo scInfo) {
        if (param == null) {
            throw new RuntimeException("request need param");
        }
        return param.checkParam(schema, scInfo);
    }

    public Set<String> checkResult(SchemaColumnInfo scInfo) {
        if (result == null) {
            throw new RuntimeException("request need result");
        }
        return result.checkResult(schema, scInfo);
    }
}
