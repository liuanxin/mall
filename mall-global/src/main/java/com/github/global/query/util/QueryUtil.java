package com.github.global.query.util;

import com.github.global.query.constant.QueryConst;
import com.github.global.query.model.Scheme;
import com.github.global.query.model.SchemeColumn;
import com.github.global.query.model.TableColumnInfo;

import java.util.Map;

public class QueryUtil {

    public static String toStr(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }

    public static boolean isNullString(Object value) {
        if (value instanceof String) {
            String str = ((String) value).trim();
            return str.isEmpty() || "null".equalsIgnoreCase(str) || "undefined".equalsIgnoreCase(str);
        }
        return false;
    }

    public static SchemeColumn checkColumnName(String column, String mainScheme,
                                               TableColumnInfo columnInfo, String type) {
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

    public static SchemeColumn checkSchemeAndColumnName(String schemeName, String columnName,
                                                        TableColumnInfo columnInfo, String type) {
        if (schemeName == null || schemeName.isEmpty()) {
            throw new RuntimeException("scheme can't be blank with: " + type);
        }
        if (columnName.isEmpty()) {
            throw new RuntimeException("scheme(" + columnName + ") column cant' be blank with: " + type);
        }

        Map<String, String> aliasMap = columnInfo.getAliasMap();
        Map<String, Scheme> schemeMap = columnInfo.getSchemeMap();
        String realSchemeName = aliasMap.get(QueryConst.SCHEME_PREFIX + schemeName);
        Scheme scheme = (realSchemeName == null || realSchemeName.isEmpty())
                ? schemeMap.get(schemeName) : schemeMap.get(realSchemeName);
        if (scheme == null) {
            throw new RuntimeException("no scheme(" + schemeName + ") defined with: " + type);
        }

        Map<String, SchemeColumn> columnMap = scheme.getColumnMap();
        String realColumnName = aliasMap.get(QueryConst.COLUMN_PREFIX + columnName);
        SchemeColumn schemeColumn = (realColumnName == null || realColumnName.isEmpty())
                ? columnMap.get(columnName) : columnMap.get(realColumnName);
        if (schemeColumn == null) {
            throw new RuntimeException("scheme(" + schemeName + ") no column(" + columnName + ") defined with: " + type);
        }
        return schemeColumn;
    }
}
