package com.github.global.query.util;

import com.github.global.query.model.Scheme;
import com.github.global.query.model.SchemeColumn;

import java.util.Map;

public class QueryUtil {

    public static SchemeColumn checkColumnName(String column, String defaultScheme,
                                               Map<String, Scheme> schemeMap, String type) {
        String schemeName, columnName;
        if (column.contains(".")) {
            String[] arr = column.split("\\.");
            schemeName = arr[0].trim();
            columnName = arr[1].trim();
        } else {
            schemeName = defaultScheme;
            columnName = column;
        }

        Scheme scheme = schemeMap.get(schemeName);
        if (scheme == null) {
            throw new RuntimeException("no scheme(" + schemeName + ") in " + type);
        }
        SchemeColumn schemeColumn = scheme.getColumnMap().get(columnName);
        if (schemeColumn == null) {
            throw new RuntimeException("scheme(" + schemeName + ") no column(" + columnName + ") in " + type);
        }
        return schemeColumn;
    }
}
