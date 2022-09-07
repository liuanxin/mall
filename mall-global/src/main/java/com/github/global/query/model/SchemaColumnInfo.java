package com.github.global.query.model;

import com.github.global.query.constant.QueryConst;
import com.github.global.query.util.QueryUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaColumnInfo {

    private Map<String, String> aliasMap;
    private Map<String, Schema> schemaMap;
    private Map<String, SchemaColumnRelation> relationMap;

    public Schema findSchema(String schemaName) {
        String schemaAlias = aliasMap.get(QueryConst.SCHEMA_PREFIX + schemaName);
        Schema schema = schemaMap.get(schemaAlias);
        return schema == null ? schemaMap.get(schemaName) : schema;
    }

    public SchemaColumnRelation findRelation(String schemaAndColumn) {
        if (relationMap.isEmpty() || schemaAndColumn == null || !schemaAndColumn.contains(".")) {
            return null;
        }

        String[] arr = schemaAndColumn.split("\\.");
        String schema = arr[0].trim();
        String column = arr[1].trim();

        String schemaAlias = aliasMap.get(QueryConst.SCHEMA_PREFIX + schema);
        String columnAlias = aliasMap.get(QueryConst.COLUMN_PREFIX + column);

        String realSchema = QueryUtil.defaultIfBlank(schemaAlias, schema);
        String realColumn = QueryUtil.defaultIfBlank(columnAlias, column);
        return relationMap.get(realSchema + "." + realColumn);
    }

    public void checkSchemaRelation(Set<String> schemaNames) {
        // todo
    }
    public void checkParamResultSchema(Set<String> paramSchemaNames, Set<String> resultSchemaNames) {
    }
}
