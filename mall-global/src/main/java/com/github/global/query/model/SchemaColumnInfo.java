package com.github.global.query.model;

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
}
