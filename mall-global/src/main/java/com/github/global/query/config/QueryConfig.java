package com.github.global.query.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueryConfig {

    @Value("${query.scan-packages:}")
    private String scanPackages;

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public QuerySchemaInfo schemaInfo() {
        return new QuerySchemaInfo(scanPackages, jdbcTemplate);
    }
}
