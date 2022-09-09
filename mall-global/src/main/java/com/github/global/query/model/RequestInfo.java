package com.github.global.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestInfo {

    /** 主结构, 如果入参和出参是相同的, 只需要在这里定义一次就可以了 */
    private String schema;
    /** 入参 */
    private ReqParam param;
    /** 出参 */
    private ReqResult result;


    public void check(SchemaColumnInfo schemaColumnInfo) {
        if (schema == null || schema.isEmpty()) {
            throw new RuntimeException("no schema");
        }
        if (schemaColumnInfo.findSchema(schema) == null) {
            throw new RuntimeException("no schema(" + schema + ") defined");
        }
        if (param == null) {
            throw new RuntimeException("request need param");
        }
        if (result == null) {
            throw new RuntimeException("request need result");
        }

        param.checkParam(schema, schemaColumnInfo);
        Set<String> paramSchema = param.allParamSchema(schema);
        paramSchema.remove(schema);
        if (paramSchema.size() > 1) {
            schemaColumnInfo.checkSchemaRelation(schema, paramSchema, "param");
        }

        result.checkResult(schema, schemaColumnInfo);
        Set<String> resultSchema = result.allResultSchema(schema);
        resultSchema.remove(schema);
        if (resultSchema.size() > 1) {
            schemaColumnInfo.checkSchemaRelation(schema, resultSchema, "result");
        }
    }
}
