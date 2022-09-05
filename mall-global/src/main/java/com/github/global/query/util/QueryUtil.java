package com.github.global.query.util;

import com.github.common.util.U;
import com.github.global.query.annotation.ColumnInfo;
import com.github.global.query.annotation.SchemeInfo;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;

public class QueryUtil {

    private static final Logger LOG = LoggerFactory.getLogger(QueryUtil.class);

    private static final PathMatchingResourcePatternResolver RESOLVER =
            new PathMatchingResourcePatternResolver(ClassLoader.getSystemClassLoader());

    private static final MetadataReaderFactory READER = new CachingMetadataReaderFactory(RESOLVER);


    public static TableColumnInfo scanScheme(String classPackages) {
        return handleTable(scanPackage(classPackages));
    }

    private static Set<Class<?>> scanPackage(String classPackages) {
        if (classPackages == null || classPackages.trim().isEmpty()) {
            return Collections.emptySet();
        }
        String[] paths = StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(classPackages));
        if (paths.length == 0) {
            return Collections.emptySet();
        }

        Set<Class<?>> set = new LinkedHashSet<>();
        for (String path : paths) {
            try {
                String location = String.format("classpath*:**/%s/**/*.class", path.replace(".", "/"));
                for (Resource resource : RESOLVER.getResources(location)) {
                    if (resource.isReadable()) {
                        String className = READER.getMetadataReader(resource).getClassMetadata().getClassName();
                        set.add(Class.forName(className));
                    }
                }
            } catch (Exception e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("get({}) class exception", path, e);
                }
            }
        }
        return set;
    }

    private static TableColumnInfo handleTable(Set<Class<?>> classes) {
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, Scheme> schemeMap = new LinkedHashMap<>();
        Map<String, TableColumnRelation> relationMap = new HashMap<>();

        Map<String, ColumnInfo> columnInfoMap = new LinkedHashMap<>();
        for (Class<?> clazz : classes) {
            SchemeInfo schemeInfo = clazz.getAnnotation(SchemeInfo.class);
            String schemeName, schemeDesc, schemeAlias;
            if (schemeInfo != null) {
                if (schemeInfo.ignore()) {
                    continue;
                }

                schemeName = schemeInfo.value();
                schemeDesc = schemeInfo.desc();
                schemeAlias = schemeInfo.alias();
            } else {
                schemeDesc = "";
                schemeAlias = clazz.getSimpleName();
                schemeName = convertTableName(schemeAlias);
            }
            if (schemeMap.containsKey(schemeAlias)) {
                throw new RuntimeException("scheme(" + schemeName + ") has renamed");
            }

            Map<String, SchemeColumn> columnMap = new LinkedHashMap<>();
            for (Field field : U.getFields(clazz)) {
                ColumnInfo columnInfo = field.getAnnotation(ColumnInfo.class);
                String columnName, columnDesc, columnAlias;
                boolean primary;
                if (columnInfo != null) {
                    if (columnInfo.ignore()) {
                        continue;
                    }

                    columnName = columnInfo.value();
                    columnDesc = columnInfo.desc();
                    columnAlias = columnInfo.alias();
                    primary = columnInfo.primary();

                    columnInfoMap.put(schemeName + "." + columnName, columnInfo);
                } else {
                    columnDesc = "";
                    columnAlias = field.getName();
                    columnName = convertColumnName(columnAlias);
                    primary = "id".equalsIgnoreCase(field.getName());
                }
                if (columnMap.containsKey(columnAlias)) {
                    throw new RuntimeException("scheme(" + schemeName + ") has same column(" + columnAlias + ")");
                }

                SchemeColumn column = new SchemeColumn(columnName, columnDesc, columnAlias, primary, field.getType());
                aliasMap.put(QueryConst.COLUMN_PREFIX + columnName, columnAlias);
                columnMap.put(columnAlias, column);
            }
            aliasMap.put(QueryConst.SCHEME_PREFIX + schemeName, schemeAlias);
            schemeMap.put(schemeAlias, new Scheme(schemeName, schemeDesc, schemeAlias, columnMap));
        }

        for (Map.Entry<String, ColumnInfo> entry : columnInfoMap.entrySet()) {
            ColumnInfo columnInfo = entry.getValue();
            SchemeRelationType relationType = columnInfo.relationType();
            if (relationType != SchemeRelationType.NULL) {
                String relationScheme = columnInfo.relationScheme();
                String relationColumn = columnInfo.relationColumn();
                if (!relationScheme.isEmpty() && !relationColumn.isEmpty()) {
                    String schemeAndColumn = entry.getKey();
                    String realSchemeName = aliasMap.get(QueryConst.SCHEME_PREFIX + relationScheme);
                    Scheme scheme = (realSchemeName == null || realSchemeName.isEmpty())
                            ? schemeMap.get(relationScheme) : schemeMap.get(realSchemeName);
                    if (scheme == null) {
                        throw new RuntimeException(schemeAndColumn + "'s relation no scheme(" + relationScheme + ")");
                    }

                    Map<String, SchemeColumn> columnMap = scheme.getColumnMap();
                    String realColumnName = aliasMap.get(QueryConst.COLUMN_PREFIX + relationColumn);
                    boolean exists = (realColumnName == null || realColumnName.isEmpty())
                            ? columnMap.containsKey(relationColumn) : columnMap.containsKey(realColumnName);
                    if (!exists) {
                        throw new RuntimeException(schemeAndColumn + "'s relation no scheme-column("
                                + relationScheme + "." + relationColumn + ")");
                    }
                    relationMap.put(schemeAndColumn, new TableColumnRelation(relationType, relationScheme));
                }
            }
        }

        return new TableColumnInfo(aliasMap, schemeMap, relationMap);
    }

    private static String convertTableName(String className) {
        StringBuilder sbd = new StringBuilder();
        char[] chars = className.toCharArray();
        int len = chars.length;
        for (int i = 0; i < len; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sbd.append("_");
                }
                sbd.append(Character.toLowerCase(c));
            } else {
                sbd.append(c);
            }
        }
        return sbd.toString();
    }
    private static String convertColumnName(String fieldName) {
        StringBuilder sbd = new StringBuilder();
        for (char c : fieldName.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sbd.append("_").append(Character.toLowerCase(c));
            } else {
                sbd.append(c);
            }
        }
        return sbd.toString();
    }

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
