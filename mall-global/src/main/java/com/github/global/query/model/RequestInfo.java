package com.github.global.query.model;

import com.github.global.query.enums.ReqResultType;
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
    /** 出参的结构类型, 对象(obj)还是数组(arr), 不设置则默认是数组 */
    private ReqResultType type;
    /** 出参 */
    private ReqResult result;


    public Set<String> check(SchemaColumnInfo schemaColumnInfo) {
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

        Set<String> paramSchemaSet = param.checkParam(schema, schemaColumnInfo);
        paramSchemaSet.remove(schema);
        Set<String> resultSchema = result.checkResult(schema, schemaColumnInfo, paramSchemaSet);
        paramSchemaSet.addAll(resultSchema);
        return paramSchemaSet;
    }
}
