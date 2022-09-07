package com.github.global.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
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


    public void check(SchemaColumnInfo columnInfo) {
        if (schema != null && !schema.isEmpty()) {
            Map<String, Schema> schemaMap = columnInfo.getSchemaMap();
            if (!schemaMap.containsKey(schema)) {
                throw new RuntimeException("no schema(" + schema + ") defined");
            }
        }

        if (param == null) {
            throw new RuntimeException("request need param");
        }
        if (result == null) {
            throw new RuntimeException("request need result");
        }

        param.checkParam(schema, columnInfo);
        Set<String> paramSchema = param.allParamSchema(schema, columnInfo);
        if (paramSchema.size() > 1) {
            columnInfo.checkSchemaRelation(paramSchema);
        }

        result.checkResult(schema, columnInfo);
        Set<String> resultSchema = result.allResultSchema(schema, columnInfo);
        if (paramSchema.size() > 1) {
            columnInfo.checkSchemaRelation(paramSchema);
        }

        if (paramSchema.size() > 1 && resultSchema.size() > 1) {
            columnInfo.checkParamResultSchema(paramSchema, resultSchema);
        }
    }
}
