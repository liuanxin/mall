package com.github.global.query.util;

import com.github.global.query.constant.QueryConst;
import com.github.global.query.model.Schema;
import com.github.global.query.model.SchemaColumn;
import com.github.global.query.model.SchemaColumnInfo;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

public class QueryDbUtil {

    public static SchemaColumnInfo scanSchema(JdbcTemplate jdbcTemplate) {
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, Schema> schemaMap = new LinkedHashMap<>();

        String dbName = jdbcTemplate.queryForObject(QueryConst.DB_SQL, String.class);
        List<Map<String, Object>> schemaList = jdbcTemplate.queryForList(QueryConst.SCHEMA_SQL, dbName);
        for (Map<String, Object> schemaInfo : schemaList) {
            String schemaName = QueryUtil.toStr(schemaInfo.get("tn"));
            String schemaDesc = QueryUtil.toStr(schemaInfo.get("tc"));
            Map<String, SchemaColumn> columnMap = new LinkedHashMap<>();

            List<Map<String, Object>> columnList = jdbcTemplate.queryForList(QueryConst.COLUMN_SQL, dbName, schemaName);
            for (Map<String, Object> columnInfo : columnList) {
                String columnName = QueryUtil.toStr(columnInfo.get("cn"));
                String columnType = QueryUtil.toStr(columnInfo.get("ct"));
                String columnDesc = QueryUtil.toStr(columnInfo.get("cc"));
                boolean primary = "PRI".equalsIgnoreCase(QueryUtil.toStr(columnInfo.get("cc")));

                SchemaColumn column = new SchemaColumn(columnName, columnDesc, columnName, primary, mappingClass(columnType));
                aliasMap.put(QueryConst.COLUMN_PREFIX + columnName, columnName);
                columnMap.put(columnName, column);
            }
            aliasMap.put(QueryConst.SCHEMA_PREFIX + schemaName, schemaName);
            schemaMap.put(schemaName, new Schema(schemaName, schemaDesc, schemaName, columnMap));
        }
        return new SchemaColumnInfo(aliasMap, schemaMap, Collections.emptyMap());
    }

    private static Class<?> mappingClass(String dbType) {
        if ("tinyint(1)".equalsIgnoreCase(dbType)) {
            return Boolean.class;
        } else {
            String type = dbType.contains("(") ? dbType.substring(0, dbType.indexOf("(")) : dbType;
            return QueryConst.DB_TYPE_MAP.get(type.toLowerCase());
        }
    }
}
