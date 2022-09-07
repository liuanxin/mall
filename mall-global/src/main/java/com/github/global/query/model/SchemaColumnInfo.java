package com.github.global.query.model;

import com.github.global.query.constant.QueryConst;
import com.github.global.query.util.QueryUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaColumnInfo {

    private Map<String, String> aliasMap;
    private Map<String, Schema> schemaMap;
    private Map<String, SchemaColumnRelation> relationMap;

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
}
