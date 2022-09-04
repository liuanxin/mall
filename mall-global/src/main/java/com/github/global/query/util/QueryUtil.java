package com.github.global.query.util;

import com.github.global.query.model.Scheme;
import com.github.global.query.model.SchemeColumn;
import com.github.global.query.model.TableColumnInfo;

import java.util.Map;

public class QueryUtil {

    public static String toStr(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }

    public static SchemeColumn checkColumnName(String column, String mainScheme, TableColumnInfo columnInfo, String type) {
        String schemeName, columnName;
        if (column.contains(".")) {
            String[] arr = column.split("\\.");
            schemeName = arr[0].trim();
            columnName = arr[1].trim();
        } else {
            schemeName = mainScheme;
            columnName = column.trim();
        }
        return checkSchemeAndColumnName(schemeName, columnName, columnInfo, type);
    }

    public static SchemeColumn checkSchemeAndColumnName(String schemeName, String columnName, TableColumnInfo columnInfo, String type) {
        if (schemeName == null || schemeName.isEmpty()) {
            throw new RuntimeException("scheme can't be blank with: " + type);
        }
        if (columnName.isEmpty()) {
            throw new RuntimeException("scheme(" + columnName + ") column cant' be blank with: " + type);
        }

        Map<String, String> aliasMap = columnInfo.getAliasMap();
        Map<String, Scheme> schemeMap = columnInfo.getSchemeMap();
        Scheme scheme = schemeMap.getOrDefault(schemeName, schemeMap.get(aliasMap.get(schemeName)));
        if (scheme == null) {
            throw new RuntimeException("no scheme(" + schemeName + ") defined with: " + type);
        }
        SchemeColumn schemeColumn = scheme.getColumnMap().get(columnName);
        if (schemeColumn == null) {
            throw new RuntimeException("scheme(" + schemeName + ") no column(" + columnName + ") defined with: " + type);
        }
        return schemeColumn;
    }
}
