package com.github.global.query.config;

import com.github.global.query.DynamicQueryHandler;
import com.github.global.query.model.TableColumnInfo;

public class QuerySchemeInfo {

    private final TableColumnInfo tableColumnInfo;

    public QuerySchemeInfo(String scanPackages) {
        tableColumnInfo = DynamicQueryHandler.scanScheme(scanPackages);
    }

    public TableColumnInfo getTableColumnInfo() {
        return tableColumnInfo;
    }
}
