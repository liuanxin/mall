package com.github.global.query.config;

import com.github.global.query.model.SchemaColumnInfo;
import com.github.global.query.util.QueryDbUtil;
import com.github.global.query.util.QueryUtil;
import org.springframework.jdbc.core.JdbcTemplate;

public class QuerySchemaInfo {

    private final SchemaColumnInfo schemaColumnInfo;

    public QuerySchemaInfo(String scanPackages, JdbcTemplate jdbcTemplate) {
        SchemaColumnInfo info = QueryUtil.scanSchema(scanPackages);
        schemaColumnInfo = info.getSchemaMap().isEmpty() ? QueryDbUtil.scanSchema(jdbcTemplate) : info;
    }

    public SchemaColumnInfo getTableColumnInfo() {
        return schemaColumnInfo;
    }
}
