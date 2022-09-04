package com.github.global.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableColumnInfo {

    private Map<String, String> aliasMap;
    private Map<String, Scheme> schemeMap;
    private Map<String, TableColumnRelation> relationMap;
}
