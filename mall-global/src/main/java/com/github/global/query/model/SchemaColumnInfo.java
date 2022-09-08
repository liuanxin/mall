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
    private Map<String, Map<String, List<SchemaColumnRelation>>> masterRelationMap;
    private Map<String, Map<String, SchemaColumnRelation>> childRelationMap;

    public SchemaColumnInfo(Map<String, String> aliasMap, Map<String, Schema> schemaMap, List<SchemaColumnRelation> relationList) {
        this.aliasMap = aliasMap;
        this.schemaMap = schemaMap;
        fillRelation(relationList);
    }
    private void fillRelation(List<SchemaColumnRelation> relationList) {
        Map<String, Map<String, List<SchemaColumnRelation>>> masterMap = new HashMap<>();
        Map<String, Map<String, SchemaColumnRelation>> childMap = new HashMap<>();
        if (relationList != null && !relationList.isEmpty()) {
            for (SchemaColumnRelation relation : relationList) {
                String masterSchema = relation.getOneSchema();
                String masterColumn = relation.getOneColumn();
                String childSchema = relation.getOneOrManySchema();
                String childColumn = relation.getOneOrManyColumn();

                Map<String, List<SchemaColumnRelation>> masterRelation = masterMap.getOrDefault(masterSchema, new HashMap<>());
                List<SchemaColumnRelation> masterColumnList = masterRelation.getOrDefault(masterColumn, new ArrayList<>());
                masterColumnList.add(relation);
                masterRelation.put(masterColumn, masterColumnList);
                masterMap.put(masterSchema, masterRelation);

                Map<String, SchemaColumnRelation> childRelation = childMap.getOrDefault(childSchema, new HashMap<>());
                childRelation.put(childColumn, relation);
                childMap.put(childColumn, childRelation);
            }
        }
        masterRelationMap = masterMap;
        childRelationMap = childMap;
    }

    public Schema findSchema(String schemaName) {
        String schemaAlias = aliasMap.get(QueryConst.SCHEMA_PREFIX + schemaName);
        Schema schema = schemaMap.get(schemaAlias);
        return schema == null ? schemaMap.get(schemaName) : schema;
    }

    public List<SchemaColumnRelation> findRelationByMaster(String schemaAndColumn) {
        if (masterRelationMap == null || masterRelationMap.isEmpty() || schemaAndColumn == null || !schemaAndColumn.contains(".")) {
            return Collections.emptyList();
        }

        String[] arr = schemaAndColumn.split("\\.");
        String schema = arr[0].trim();
        String column = arr[1].trim();

        String schemaAlias = aliasMap.get(QueryConst.SCHEMA_PREFIX + schema);
        Map<String, List<SchemaColumnRelation>> relationMap = masterRelationMap.get(schemaAlias);
        if (relationMap == null || relationMap.isEmpty()) {
            relationMap = masterRelationMap.get(schema);
        }
        if (relationMap == null || relationMap.isEmpty()) {
            return null;
        }

        String columnAlias = aliasMap.get(QueryConst.COLUMN_PREFIX + column);
        List<SchemaColumnRelation> relationList = relationMap.get(columnAlias);
        return (relationList == null) ? relationMap.get(column) : relationList;
    }

    public SchemaColumnRelation findRelationByChild(String schemaAndColumn) {
        if (childRelationMap == null || childRelationMap.isEmpty() || schemaAndColumn == null || !schemaAndColumn.contains(".")) {
            return null;
        }

        String[] arr = schemaAndColumn.split("\\.");
        String schema = arr[0].trim();
        String column = arr[1].trim();

        String schemaAlias = aliasMap.get(QueryConst.SCHEMA_PREFIX + schema);
        Map<String, SchemaColumnRelation> relationMap = childRelationMap.get(schemaAlias);
        if (relationMap == null || relationMap.isEmpty()) {
            relationMap = childRelationMap.get(schema);
        }
        if (relationMap == null || relationMap.isEmpty()) {
            return null;
        }

        String columnAlias = aliasMap.get(QueryConst.COLUMN_PREFIX + column);
        SchemaColumnRelation relation = relationMap.get(columnAlias);
        return (relation == null) ? relationMap.get(column) : relation;
    }

    public void checkSchemaRelation(Set<String> schemaNames) {
        // todo
    }
    public void checkParamResultSchema(Set<String> paramSchemaNames, Set<String> resultSchemaNames) {
    }
}
