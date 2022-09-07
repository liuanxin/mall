package com.github.global.query.util;

import com.github.common.util.U;
import com.github.global.query.annotation.ColumnInfo;
import com.github.global.query.annotation.SchemaInfo;
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


    public static SchemaColumnInfo scanSchema(String classPackages) {
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
    private static SchemaColumnInfo handleTable(Set<Class<?>> classes) {
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, Schema> schemaMap = new LinkedHashMap<>();
        Map<String, SchemaColumnRelation> relationMap = new HashMap<>();

        Map<String, ColumnInfo> columnInfoMap = new LinkedHashMap<>();
        Map<String, Class<?>> columnClassMap = new HashMap<>();
        for (Class<?> clazz : classes) {
            SchemaInfo schemaInfo = clazz.getAnnotation(SchemaInfo.class);
            String schemaName, schemaDesc, schemaAlias;
            if (schemaInfo != null) {
                if (schemaInfo.ignore()) {
                    continue;
                }

                schemaName = schemaInfo.value();
                schemaDesc = schemaInfo.desc();
                schemaAlias = QueryUtil.defaultIfBlank(schemaInfo.alias(), schemaName);
            } else {
                schemaDesc = "";
                schemaAlias = clazz.getSimpleName();
                schemaName = aliasToSchemaName(schemaAlias);
            }
            if (schemaMap.containsKey(schemaAlias)) {
                throw new RuntimeException("schema(" + schemaName + ") has renamed");
            }

            Map<String, SchemaColumn> columnMap = new LinkedHashMap<>();
            for (Field field : U.getFields(clazz)) {
                ColumnInfo columnInfo = field.getAnnotation(ColumnInfo.class);
                Class<?> type = field.getType();
                String columnName, columnDesc, columnAlias;
                boolean primary;
                if (columnInfo != null) {
                    if (columnInfo.ignore()) {
                        continue;
                    }

                    columnName = columnInfo.value();
                    columnDesc = columnInfo.desc();
                    columnAlias = QueryUtil.defaultIfBlank(columnInfo.alias(), columnName);
                    primary = columnInfo.primary();

                    String schemaAndColumn = schemaAlias + "." + columnAlias;
                    columnInfoMap.put(schemaAndColumn, columnInfo);
                    columnClassMap.put(schemaAndColumn, type);
                } else {
                    columnDesc = "";
                    columnAlias = field.getName();
                    columnName = aliasToColumnName(columnAlias);
                    primary = "id".equalsIgnoreCase(columnAlias);
                }
                if (columnMap.containsKey(columnAlias)) {
                    throw new RuntimeException("schema(" + schemaName + ") has same column(" + columnAlias + ")");
                }

                aliasMap.put(QueryConst.COLUMN_PREFIX + columnName, columnAlias);
                columnMap.put(columnAlias, new SchemaColumn(columnName, columnDesc, columnAlias, primary, type));
            }
            aliasMap.put(QueryConst.SCHEMA_PREFIX + schemaName, schemaAlias);
            schemaMap.put(schemaAlias, new Schema(schemaName, schemaDesc, schemaAlias, columnMap));
        }

        for (Map.Entry<String, ColumnInfo> entry : columnInfoMap.entrySet()) {
            ColumnInfo columnInfo = entry.getValue();
            SchemaRelationType relationType = columnInfo.relationType();
            if (relationType != SchemaRelationType.NULL) {
                String relationSchema = columnInfo.relationSchema();
                String relationColumn = columnInfo.relationColumn();
                if (!relationSchema.isEmpty() && !relationColumn.isEmpty()) {
                    String schemaAndColumn = entry.getKey();
                    String realSchemaName = aliasMap.get(QueryConst.SCHEMA_PREFIX + relationSchema);
                    Schema schema = schemaMap.get(defaultIfBlank(realSchemaName, relationSchema));
                    if (schema == null) {
                        throw new RuntimeException(schemaAndColumn + "'s relation no schema(" + relationSchema + ")");
                    }

                    Map<String, SchemaColumn> columnMap = schema.getColumnMap();
                    String realColumnName = aliasMap.get(QueryConst.COLUMN_PREFIX + relationColumn);
                    SchemaColumn column = columnMap.get(defaultIfBlank(realColumnName, relationColumn));
                    if (column == null) {
                        throw new RuntimeException(schemaAndColumn + "'s relation no schema-column("
                                + relationSchema + "." + relationColumn + ")");
                    }
                    Class<?> sourceClass = columnClassMap.get(schemaAndColumn);
                    Class<?> targetClass = column.getColumnType();
                    if (sourceClass != targetClass) {
                        throw new RuntimeException(schemaAndColumn + "'s data type has " + sourceClass.getSimpleName()
                                + ", but relation " + relationSchema + "'s data type has" + targetClass.getSimpleName());
                    }
                    relationMap.put(schemaAndColumn, new SchemaColumnRelation(relationType, relationSchema, relationColumn));
                }
            }
        }
        return new SchemaColumnInfo(aliasMap, schemaMap, relationMap);
    }
    /** UserInfo --> user_info */
    private static String aliasToSchemaName(String className) {
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
    /** userName --> user_name */
    private static String aliasToColumnName(String fieldName) {
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

    /** user_info | USER_INFO --> UserInfo */
    public static String schemaNameToAlias(String schemaName) {
        if (schemaName.toLowerCase().startsWith("t_")) {
            schemaName = schemaName.substring(2);
        }
        StringBuilder sbd = new StringBuilder();
        char[] chars = schemaName.toCharArray();
        sbd.append(Character.toUpperCase(chars[0]));
        int len = chars.length;
        for (int i = 1; i < len; i++) {
            char c = chars[i];
            if (c == '_') {
                i++;
                sbd.append(Character.toUpperCase(chars[i]));
            } else {
                sbd.append(Character.toLowerCase(c));
            }
        }
        return sbd.toString();
    }
    /** user_name | USER_NAME --> userName */
    public static String columnNameToAlias(String columnName) {
        StringBuilder sbd = new StringBuilder();
        char[] chars = columnName.toCharArray();
        int len = chars.length;
        for (int i = 0; i < len; i++) {
            char c = chars[i];
            if (c == '_') {
                i++;
                sbd.append(Character.toUpperCase(chars[i]));
            } else {
                sbd.append(Character.toLowerCase(c));
            }
        }
        return sbd.toString();
    }


    public static String toStr(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }

    public static String defaultIfBlank(String str1, String defaultStr) {
        return (str1 == null || str1.isEmpty()) ? defaultStr : str1;
    }

    public static boolean isNullString(Object value) {
        if (value instanceof String) {
            String str = ((String) value).trim();
            return str.isEmpty() || "null".equalsIgnoreCase(str) || "undefined".equalsIgnoreCase(str);
        }
        return false;
    }

    public static String getSchemaName(String column, String mainSchema) {
        return column.contains(".") ? column.split("\\.")[0].trim() : mainSchema;
    }

    public static String getColumnName(String column) {
        return column.contains(".") ? column.split("\\.")[1].trim() : column.trim();
    }

    public static SchemaColumn checkColumnName(String column, String mainSchema,
                                               SchemaColumnInfo columnInfo, String type) {
        String schemaName = getSchemaName(column, mainSchema);
        String columnName = getColumnName(column);
        return checkSchemaAndColumnName(schemaName, columnName, columnInfo, type);
    }

    public static SchemaColumn checkSchemaAndColumnName(String schemaName, String columnName,
                                                        SchemaColumnInfo columnInfo, String type) {
        Map<String, String> aliasMap = columnInfo.getAliasMap();
        Schema schema = querySchema(type, schemaName, aliasMap, columnInfo.getSchemaMap());
        return queryColumn(type, schemaName, columnName, aliasMap, schema.getColumnMap());
    }

    public static Schema querySchema(String type, String schemaName, Map<String, String> aliasMap,
                                     Map<String, Schema> schemaMap) {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new RuntimeException("schema can't be blank with: " + type);
        }

        String realSchemaName = aliasMap.get(QueryConst.SCHEMA_PREFIX + schemaName);
        Schema schema = (realSchemaName == null || realSchemaName.isEmpty())
                ? schemaMap.get(schemaName) : schemaMap.get(realSchemaName);
        if (schema == null) {
            throw new RuntimeException("no schema(" + schemaName + ") defined with: " + type);
        }
        return schema;
    }

    public static SchemaColumn queryColumn(String type, String schemaName, String columnName,
                                           Map<String, String> aliasMap, Map<String, SchemaColumn> columnMap) {
        if (columnName.isEmpty()) {
            throw new RuntimeException("schema(" + columnName + ") column cant' be blank with: " + type);
        }

        String realColumnName = aliasMap.get(QueryConst.COLUMN_PREFIX + columnName);
        SchemaColumn schemaColumn = (realColumnName == null || realColumnName.isEmpty())
                ? columnMap.get(columnName) : columnMap.get(realColumnName);
        if (schemaColumn == null) {
            throw new RuntimeException("schema(" + schemaName + ") no column(" + columnName + ") defined with: " + type);
        }
        return schemaColumn;
    }
}
