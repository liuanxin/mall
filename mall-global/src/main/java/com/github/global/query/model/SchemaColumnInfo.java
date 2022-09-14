package com.github.global.query.model;

import com.github.global.query.constant.QueryConst;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaColumnInfo {

    private Map<String, String> aliasMap;
    private Map<String, Schema> schemaMap;

    private Map<String, Map<String, SchemaColumnRelation>> childRelationMap;
    private Map<String, Map<String, SchemaColumnRelation>> masterChildSchemaMap;

    public SchemaColumnInfo(Map<String, String> aliasMap, Map<String, Schema> schemaMap, List<SchemaColumnRelation> relationList) {
        this.aliasMap = aliasMap;
        this.schemaMap = schemaMap;
        fillRelation(relationList);
    }
    private void fillRelation(List<SchemaColumnRelation> relationList) {
        Map<String, Map<String, SchemaColumnRelation>> childMap = new HashMap<>();
        Map<String, Map<String, SchemaColumnRelation>> schemaMasterChildMap = new HashMap<>();
        if (relationList != null && !relationList.isEmpty()) {
            for (SchemaColumnRelation relation : relationList) {
                String masterSchema = relation.getOneSchema();
                String childSchema = relation.getOneOrManySchema();
                String childColumn = relation.getOneOrManyColumn();

                Map<String, SchemaColumnRelation> childRelation = childMap.getOrDefault(childSchema, new HashMap<>());
                childRelation.put(childColumn, relation);
                childMap.put(childColumn, childRelation);

                Map<String, SchemaColumnRelation> masterChildRelation = schemaMasterChildMap.getOrDefault(masterSchema, new HashMap<>());
                masterChildRelation.put(childSchema, relation);
                schemaMasterChildMap.put(masterSchema, masterChildRelation);
            }
        }
        childRelationMap = childMap;
        masterChildSchemaMap = schemaMasterChildMap;
    }

    public Schema findSchema(String schemaName) {
        String schemaAlias = aliasMap.get(QueryConst.SCHEMA_PREFIX + schemaName);
        Schema schema = schemaMap.get(schemaAlias);
        return schema == null ? schemaMap.get(schemaName) : schema;
    }

    public SchemaColumn findSchemaColumn(Schema schema, String columnName) {
        Map<String, SchemaColumn> columnMap = schema.getColumnMap();
        String columnAlias = aliasMap.get(QueryConst.COLUMN_PREFIX + columnName);
        SchemaColumn schemaColumn = columnMap.get(columnAlias);
        return schemaColumn == null ? columnMap.get(columnName) : schemaColumn;
    }

    public SchemaColumn findSchemaColumn(String schemaName, String columnName) {
        Schema schema = findSchema(schemaName);
        return (schema == null) ? null : findSchemaColumn(schema, columnName);
    }

    public SchemaColumnRelation findRelationByMasterChild(String masterSchema, String childSchema) {
        if (masterChildSchemaMap == null || masterChildSchemaMap.isEmpty()
                || masterSchema == null || masterSchema.isEmpty()
                || childSchema == null || childSchema.isEmpty()) {
            return null;
        }

        // noinspection DuplicatedCode
        String schemaAlias = aliasMap.get(QueryConst.SCHEMA_PREFIX + masterSchema);
        Map<String, SchemaColumnRelation> relationMap = masterChildSchemaMap.get(schemaAlias);
        Map<String, SchemaColumnRelation> useRelationMap =
                (relationMap == null || relationMap.isEmpty()) ? masterChildSchemaMap.get(masterSchema) : relationMap;
        if (useRelationMap == null || useRelationMap.isEmpty()) {
            return null;
        }

        String childSchemaAlias = aliasMap.get(QueryConst.SCHEMA_PREFIX + childSchema);
        SchemaColumnRelation relation = useRelationMap.get(childSchemaAlias);
        return (relation == null) ? useRelationMap.get(childSchema) : relation;
    }

    public SchemaColumnRelation findRelationByChild(String childSchemaAndColumn) {
        if (childRelationMap == null || childRelationMap.isEmpty()
                || childSchemaAndColumn == null || !childSchemaAndColumn.contains(".")) {
            return null;
        }

        String[] arr = childSchemaAndColumn.split("\\.");
        String schema = arr[0].trim();
        String column = arr[1].trim();

        // noinspection DuplicatedCode
        String schemaAlias = aliasMap.get(QueryConst.SCHEMA_PREFIX + schema);
        Map<String, SchemaColumnRelation> relationMap = childRelationMap.get(schemaAlias);
        Map<String, SchemaColumnRelation> useRelationMap =
                (relationMap == null || relationMap.isEmpty()) ? childRelationMap.get(schema) : relationMap;
        if (useRelationMap == null || useRelationMap.isEmpty()) {
            return null;
        }

        String columnAlias = aliasMap.get(QueryConst.COLUMN_PREFIX + column);
        SchemaColumnRelation relation = useRelationMap.get(columnAlias);
        return (relation == null) ? useRelationMap.get(column) : relation;
    }

    public void checkSchemaRelation(String mainSchema, Set<String> schemaNames, String type) {
        List<String> errorSchemaList = new ArrayList<>();
        List<String> errorRelationSchemaList = new ArrayList<>();
        for (String schemaName : schemaNames) {
            if (findSchema(schemaName) == null) {
                errorSchemaList.add(schemaName);
            }
            if (findRelationByMasterChild(mainSchema, schemaName) == null) {
                errorRelationSchemaList.add(schemaName);
            }
        }
        if (!errorSchemaList.isEmpty()) {
            throw new RuntimeException("no relation " + errorSchemaList + " defined with " + type);
        }
        if (!errorRelationSchemaList.isEmpty()) {
            throw new RuntimeException(mainSchema + " is not related to " + errorRelationSchemaList + " with " + type);
        }
    }
}
