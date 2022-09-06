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

        String dbName = jdbcTemplate.queryForObject(QueryConst.DB_SQL, String.class);
        List<Map<String, Object>> schemaList = jdbcTemplate.queryForList(QueryConst.SCHEMA_SQL, dbName);
        List<Map<String, Object>> schemaColumnList = jdbcTemplate.queryForList(QueryConst.COLUMN_SQL, dbName);

        MapMultiValue<String, Map<String, Object>, List<Map<String, Object>>> schemaColumnMap = MapMultiUtil.createMapList();
        for (Map<String, Object> schemaColumn : schemaColumnList) {
            schemaColumnMap.put(QueryUtil.toStr(schemaColumn.get("tn")), schemaColumn);
        }

        for (Map<String, Object> schemaInfo : schemaList) {
            String schemaName = QueryUtil.toStr(schemaInfo.get("tn"));
            String schemaDesc = QueryUtil.toStr(schemaInfo.get("tc"));
            Map<String, SchemaColumn> columnMap = new LinkedHashMap<>();

            List<Map<String, Object>> columnList = schemaColumnMap.get(schemaName);
            for (Map<String, Object> columnInfo : columnList) {
                Class<?> clazz = mappingClass(QueryUtil.toStr(columnInfo.get("ct")));
                String columnName = QueryUtil.toStr(columnInfo.get("cn"));
                String columnDesc = QueryUtil.toStr(columnInfo.get("cc"));
                boolean primary = "PRI".equalsIgnoreCase(QueryUtil.toStr(columnInfo.get("cc")));

                SchemaColumn column = new SchemaColumn(columnName, columnDesc, columnName, primary, clazz);
                aliasMap.put(QueryConst.COLUMN_PREFIX + columnName, columnName);
                columnMap.put(columnName, column);
            }
            aliasMap.put(QueryConst.SCHEMA_PREFIX + schemaName, schemaName);
            schemaMap.put(schemaName, new Schema(schemaName, schemaDesc, schemaName, columnMap));
        }
        return new SchemaColumnInfo(aliasMap, schemaMap, Collections.emptyMap());
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
        return null;
    }
}
