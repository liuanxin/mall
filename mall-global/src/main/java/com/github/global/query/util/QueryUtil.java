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

    public static String getSchemeName(String column, String mainScheme) {
        return column.contains(".") ? column.split("\\.")[0].trim() : mainScheme;
    }

    public static String getColumnName(String column) {
        return column.contains(".") ? column.split("\\.")[1].trim() : column.trim();
    }

    public static SchemeColumn checkColumnName(String column, String mainScheme,
                                               TableColumnInfo columnInfo, String type) {
        String schemeName = getSchemeName(column, mainScheme);
        String columnName = getColumnName(column);
        return checkSchemeAndColumnName(schemeName, columnName, columnInfo, type);
    }

    public static SchemeColumn checkSchemeAndColumnName(String schemeName, String columnName,
                                                        TableColumnInfo columnInfo, String type) {
        Map<String, String> aliasMap = columnInfo.getAliasMap();
        Scheme scheme = queryScheme(type, schemeName, aliasMap, columnInfo.getSchemeMap());
        return queryColumn(type, schemeName, columnName, aliasMap, scheme.getColumnMap());
    }

    public static Scheme queryScheme(String type, String schemeName, Map<String, String> aliasMap,
                                     Map<String, Scheme> schemeMap) {
        if (schemeName == null || schemeName.isEmpty()) {
            throw new RuntimeException("scheme can't be blank with: " + type);
        }

        String realSchemeName = aliasMap.get(QueryConst.SCHEME_PREFIX + schemeName);
        Scheme scheme = (realSchemeName == null || realSchemeName.isEmpty())
                ? schemeMap.get(schemeName) : schemeMap.get(realSchemeName);
        if (scheme == null) {
            throw new RuntimeException("no scheme(" + schemeName + ") defined with: " + type);
        }
        return scheme;
    }

    public static SchemeColumn queryColumn(String type, String schemeName, String columnName,
                                           Map<String, String> aliasMap, Map<String, SchemeColumn> columnMap) {
        if (columnName.isEmpty()) {
            throw new RuntimeException("scheme(" + columnName + ") column cant' be blank with: " + type);
        }

        String realColumnName = aliasMap.get(QueryConst.COLUMN_PREFIX + columnName);
        SchemeColumn schemeColumn = (realColumnName == null || realColumnName.isEmpty())
                ? columnMap.get(columnName) : columnMap.get(realColumnName);
        if (schemeColumn == null) {
            throw new RuntimeException("scheme(" + schemeName + ") no column(" + columnName + ") defined with: " + type);
        }
        return schemeColumn;
    }
}
