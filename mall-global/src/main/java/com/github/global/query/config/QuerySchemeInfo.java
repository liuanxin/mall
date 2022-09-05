package com.github.global.query.config;

import com.github.global.query.model.TableColumnInfo;
import com.github.global.query.util.QueryUtil;

public class QuerySchemeInfo {

    private final TableColumnInfo tableColumnInfo;

    public QuerySchemeInfo(String scanPackages) {
        tableColumnInfo = QueryUtil.scanScheme(scanPackages);
    }

    public TableColumnInfo getTableColumnInfo() {
        return tableColumnInfo;
    }
}
