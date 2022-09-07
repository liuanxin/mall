package com.github.global.query.config;

import com.github.common.collection.MapMultiUtil;
import com.github.common.collection.MapMultiValue;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.model.*;
import com.github.global.query.util.QueryUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class QuerySchemaInfoConfig {

    @Value("${query.scan-packages:}")
    private String scanPackages;

    private final JdbcTemplate jdbcTemplate;
    private final SchemaColumnInfo schemaColumnInfo;

    public QuerySchemaInfoConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

        SchemaColumnInfo info = QueryUtil.scanSchema(scanPackages);
        schemaColumnInfo = info.getSchemaMap().isEmpty() ? initWithDb() : info;
    }
    private SchemaColumnInfo initWithDb() {
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, Schema> schemaMap = new LinkedHashMap<>();
        Map<String, SchemaColumnRelation> relationMap = new HashMap<>();

        String dbName = jdbcTemplate.queryForObject(QueryConst.DB_SQL, String.class);
        List<Map<String, Object>> schemaList = jdbcTemplate.queryForList(QueryConst.SCHEMA_SQL, dbName);
        List<Map<String, Object>> schemaColumnList = jdbcTemplate.queryForList(QueryConst.COLUMN_SQL, dbName);
        List<Map<String, Object>> relationColumnList = jdbcTemplate.queryForList(QueryConst.RELATION_SQL, dbName);
        List<Map<String, Object>> indexList = jdbcTemplate.queryForList(QueryConst.INDEX_SQL, dbName);

        MapMultiValue<String, Map<String, Object>, List<Map<String, Object>>> schemaColumnMap = MapMultiUtil.createMapList();
        if (!schemaColumnList.isEmpty()) {
            for (Map<String, Object> schemaColumn : schemaColumnList) {
                schemaColumnMap.put(QueryUtil.toStr(schemaColumn.get("tn")), schemaColumn);
            }
        }
        Map<String, Map<String, Map<String, Object>>> relationColumnMap = new HashMap<>();
        if (!relationColumnList.isEmpty()) {
            for (Map<String, Object> relationColumn : relationColumnList) {
                String schemaName = QueryUtil.toStr(relationColumn.get("tn"));
                Map<String, Map<String, Object>> columnMap = relationColumnMap.getOrDefault(schemaName, new HashMap<>());
                columnMap.put(QueryUtil.toStr(relationColumn.get("cn")), relationColumn);
                relationColumnMap.put(schemaName, columnMap);
            }
        }
        Map<String, Set<String>> columnUniqueMap = new HashMap<>();
        if (!indexList.isEmpty()) {
            for (Map<String, Object> index : indexList) {
                String schemaName = QueryUtil.toStr(index.get("tn"));
                Set<String> uniqueColumnSet = columnUniqueMap.getOrDefault(schemaName, new HashSet<>());
                uniqueColumnSet.add(QueryUtil.toStr(index.get("cn")));
                columnUniqueMap.put(schemaName, uniqueColumnSet);
            }
        }

        for (Map<String, Object> schemaInfo : schemaList) {
            String schemaName = QueryUtil.toStr(schemaInfo.get("tn"));
            String schemaAlias = QueryUtil.schemaNameToAlias(schemaName);
            String schemaDesc = QueryUtil.toStr(schemaInfo.get("tc"));
            Map<String, SchemaColumn> columnMap = new LinkedHashMap<>();

            List<Map<String, Object>> columnList = schemaColumnMap.get(schemaName);
            for (Map<String, Object> columnInfo : columnList) {
                Class<?> clazz = mappingClass(QueryUtil.toStr(columnInfo.get("ct")));
                String columnName = QueryUtil.toStr(columnInfo.get("cn"));
                String columnAlias = QueryUtil.columnNameToAlias(columnName);
                String columnDesc = QueryUtil.toStr(columnInfo.get("cc"));
                boolean primary = "PRI".equalsIgnoreCase(QueryUtil.toStr(columnInfo.get("cc")));

                aliasMap.put(QueryConst.COLUMN_PREFIX + columnName, columnAlias);
                columnMap.put(columnAlias, new SchemaColumn(columnName, columnDesc, columnAlias, primary, clazz));
            }
            aliasMap.put(QueryConst.SCHEMA_PREFIX + schemaName, schemaAlias);
            schemaMap.put(schemaAlias, new Schema(schemaName, schemaDesc, schemaAlias, columnMap));
        }

        if (!relationColumnMap.isEmpty()) {
            for (Map.Entry<String, Map<String, Map<String, Object>>> entry : relationColumnMap.entrySet()) {
                String schemaName = entry.getKey();
                Set<String> uniqueColumnSet = columnUniqueMap.get(schemaName);
                for (Map.Entry<String, Map<String, Object>> columnEntry : entry.getValue().entrySet()) {
                    String columnName = columnEntry.getKey();
                    SchemaRelationType type = uniqueColumnSet.contains(columnName)
                            ? SchemaRelationType.ONE_TO_ONE : SchemaRelationType.ONE_TO_MANY;

                    Map<String, Object> relationInfoMap = columnEntry.getValue();
                    String relationSchema = QueryUtil.toStr(relationInfoMap.get("ftn"));
                    String relationColumn = QueryUtil.toStr(relationInfoMap.get("fcn"));

                    String schemaAndColumn = schemaName + "." + columnName;
                    relationMap.put(schemaAndColumn, new SchemaColumnRelation(type, relationSchema, relationColumn));
                }
            }
        }
        return new SchemaColumnInfo(aliasMap, schemaMap, relationMap);
    }
    private Class<?> mappingClass(String dbType) {
        String type = (dbType.contains("(") ? dbType.substring(0, dbType.indexOf("(")) : dbType).toLowerCase();
        for (Map.Entry<String, Class<?>> entry : QueryConst.DB_TYPE_MAP.entrySet()) {
            if (type.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        throw new RuntimeException("unknown db type" + dbType);
    }


    public List<QueryInfo> queryInfo() {
        List<QueryInfo> queryList = new ArrayList<>();
        for (Schema schema : schemaColumnInfo.getSchemaMap().values()) {
            List<QueryInfo.QueryColumn> columnList = new ArrayList<>();
            for (SchemaColumn column : schema.getColumnMap().values()) {
                String type = column.getColumnType().getSimpleName();
                columnList.add(new QueryInfo.QueryColumn(column.getAlias(), column.getDesc(), type));
            }
            queryList.add(new QueryInfo(schema.getAlias(), schema.getDesc(), columnList));
        }
        return queryList;
    }

    public Object query(RequestInfo req) {
        req.check(schemaColumnInfo);

        // todo
        List<Object> params = new ArrayList<>();
        String mainSql = generateMainSql(req, params);
        List<Map<String, Object>> maps = jdbcTemplate.queryForList(mainSql, params);

        return null;
    }
    private String generateMainSql(RequestInfo req, List<Object> params) {
        String mainSchema = req.getSchema();
        Set<String> paramSchema = req.getParam().allParamSchema(mainSchema);
        Set<String> resultSchema = req.getResult().allResultSchema(mainSchema);
        return null;
    }
}
