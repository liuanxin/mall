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
    private Map<String, Map<String, Set<SchemaColumnRelation>>> masterRelationMap;
    private Map<String, Map<String, SchemaColumnRelation>> childRelationMap;

    public SchemaColumnInfo(Map<String, String> aliasMap, Map<String, Schema> schemaMap, List<SchemaColumnRelation> relationList) {
        this.aliasMap = aliasMap;
        this.schemaMap = schemaMap;
        fillRelation(relationList);
    }
    private void fillRelation(List<SchemaColumnRelation> relationList) {
        Map<String, Map<String, Set<SchemaColumnRelation>>> masterMap = new HashMap<>();
        Map<String, Map<String, SchemaColumnRelation>> childMap = new HashMap<>();
        if (relationList != null && !relationList.isEmpty()) {
            for (SchemaColumnRelation relation : relationList) {
                String masterSchema = relation.getOneSchema();
                String masterColumn = relation.getOneColumn();
                String childSchema = relation.getOneOrManySchema();
                String childColumn = relation.getOneOrManyColumn();

                Map<String, Set<SchemaColumnRelation>> masterRelation = masterMap.getOrDefault(masterSchema, new HashMap<>());
                Set<SchemaColumnRelation> masterColumnSet = masterRelation.getOrDefault(masterColumn, new HashSet<>());
                masterColumnSet.add(relation);
                masterRelation.put(masterColumn, masterColumnSet);
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

    public Set<SchemaColumnRelation> findRelationByMaster(String schemaAndColumn) {
        if (masterRelationMap == null || masterRelationMap.isEmpty()
                || schemaAndColumn == null || !schemaAndColumn.contains(".")) {
            return Collections.emptySet();
        }

        String[] arr = schemaAndColumn.split("\\.");
        String schema = arr[0].trim();
        String column = arr[1].trim();

        String schemaAlias = aliasMap.get(QueryConst.SCHEMA_PREFIX + schema);
        Map<String, Set<SchemaColumnRelation>> relationMap = masterRelationMap.get(schemaAlias);
        Map<String, Set<SchemaColumnRelation>> useRelationMap =
                (relationMap == null || relationMap.isEmpty()) ? masterRelationMap.get(schema) : relationMap;
        if (useRelationMap == null || useRelationMap.isEmpty()) {
            return null;
        }

        String columnAlias = aliasMap.get(QueryConst.COLUMN_PREFIX + column);
        Set<SchemaColumnRelation> relationList = useRelationMap.get(columnAlias);
        return (relationList == null) ? useRelationMap.get(column) : relationList;
    }

    public SchemaColumnRelation findRelationByChild(String schemaAndColumn) {
        if (childRelationMap == null || childRelationMap.isEmpty()
                || schemaAndColumn == null || !schemaAndColumn.contains(".")) {
            return null;
        }

        String[] arr = schemaAndColumn.split("\\.");
        String schema = arr[0].trim();
        String column = arr[1].trim();

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
        Map<String, Set<SchemaColumnRelation>> masterRelationMap = this.masterRelationMap.get(mainSchema);
        if (masterRelationMap == null || masterRelationMap.isEmpty()) {
            throw new RuntimeException(mainSchema + " no relation defined");
        }

        Set<String> relationSchema = new HashSet<>();
        for (Set<SchemaColumnRelation> relations : masterRelationMap.values()) {
            for (SchemaColumnRelation relation : relations) {
                relationSchema.add(relation.getOneOrManySchema());
            }
        }
        List<String> errorSchemaList = new ArrayList<>();
        for (String schemaName : schemaNames) {
            if (!relationSchema.contains(schemaName)) {
                errorSchemaList.add(schemaName);
            }
        }
        if (!errorSchemaList.isEmpty()) {
            throw new RuntimeException(mainSchema + " is not related to " + errorSchemaList + " with " + type);
        }
    }
}
