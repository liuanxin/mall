package com.github.global.query.util;

import com.github.common.util.U;
import com.github.global.query.annotation.ColumnInfo;
import com.github.global.query.annotation.SchemaInfo;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.enums.SchemaRelationType;
import com.github.global.query.model.Schema;
import com.github.global.query.model.SchemaColumn;
import com.github.global.query.model.SchemaColumnInfo;
import com.github.global.query.model.SchemaColumnRelation;
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
        List<SchemaColumnRelation> relationList = new ArrayList<>();

        Map<String, ColumnInfo> columnInfoMap = new LinkedHashMap<>();
        Map<String, Class<?>> columnClassMap = new HashMap<>();
        Set<String> schemaNameSet = new HashSet<>();
        Set<String> schemaAliasSet = new HashSet<>();
        Set<String> columnNameSet = new HashSet<>();
        Set<String> columnAliasSet = new HashSet<>();
        for (Class<?> clazz : classes) {
            SchemaInfo schemaInfo = clazz.getAnnotation(SchemaInfo.class);
            String schemaName, schemaDesc, schemaAlias;
            if (schemaInfo != null) {
                if (schemaInfo.ignore()) {
                    continue;
                }

                schemaName = schemaInfo.value();
                schemaDesc = schemaInfo.desc();
                schemaAlias = defaultIfBlank(schemaInfo.alias(), schemaName);
            } else {
                schemaDesc = "";
                schemaAlias = clazz.getSimpleName();
                schemaName = aliasToSchemaName(schemaAlias);
            }

            if (schemaNameSet.contains(schemaName)) {
                throw new RuntimeException("schema(" + schemaName + ") has renamed");
            }
            schemaNameSet.add(schemaName);
            if (schemaAliasSet.contains(schemaAlias)) {
                throw new RuntimeException("schema alias(" + schemaName + ") has renamed");
            }
            schemaAliasSet.add(schemaAlias);

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
                    columnAlias = defaultIfBlank(columnInfo.alias(), columnName);
                    primary = columnInfo.primary();

                    // 用类名 + 列名
                    String schemaAndColumn = schemaName + "." + columnName;
                    columnInfoMap.put(schemaAndColumn, columnInfo);
                    columnClassMap.put(schemaAndColumn, type);
                } else {
                    columnDesc = "";
                    columnAlias = field.getName();
                    columnName = aliasToColumnName(columnAlias);
                    primary = "id".equalsIgnoreCase(columnAlias);
                }

                if (columnNameSet.contains(columnName)) {
                    throw new RuntimeException("schema(" + schemaAlias + ") has same column(" + columnName + ")");
                }
                columnNameSet.add(columnName);
                if (columnAliasSet.contains(columnAlias)) {
                    throw new RuntimeException("schema(" + schemaAlias + ") has same column(" + columnAlias + ")");
                }
                columnAliasSet.add(columnAlias);

                aliasMap.put(QueryConst.COLUMN_PREFIX + columnAlias, columnName);
                columnMap.put(columnName, new SchemaColumn(columnName, columnDesc, columnAlias, primary, type));
            }
            aliasMap.put(QueryConst.SCHEMA_PREFIX + schemaAlias, schemaName);
            schemaMap.put(schemaName, new Schema(schemaName, schemaDesc, schemaAlias, columnMap));
        }

        for (Map.Entry<String, ColumnInfo> entry : columnInfoMap.entrySet()) {
            ColumnInfo columnInfo = entry.getValue();
            SchemaRelationType relationType = columnInfo.relationType();
            if (relationType != SchemaRelationType.NULL) {
                String oneSchema = columnInfo.relationSchema();
                String oneColumn = columnInfo.relationColumn();
                if (!oneSchema.isEmpty() && !oneColumn.isEmpty()) {
                    String schemaAndColumn = entry.getKey();
                    Schema schema = schemaMap.get(aliasMap.get(QueryConst.SCHEMA_PREFIX + oneSchema));
                    if (schema == null) {
                        schema = schemaMap.get(oneSchema);
                        if (schema == null) {
                            throw new RuntimeException(schemaAndColumn + "'s relation no schema(" + oneSchema + ")");
                        }
                    }

                    Map<String, SchemaColumn> columnMap = schema.getColumnMap();
                    SchemaColumn column = columnMap.get(aliasMap.get(QueryConst.COLUMN_PREFIX + oneColumn));
                    if (column == null) {
                        column = columnMap.get(oneColumn);
                        if (column == null) {
                            throw new RuntimeException(schemaAndColumn + "'s relation no schema-column("
                                    + oneSchema + "." + oneColumn + ")");
                        }
                    }
                    Class<?> sourceClass = columnClassMap.get(schemaAndColumn);
                    Class<?> targetClass = column.getColumnType();
                    if (sourceClass != targetClass) {
                        throw new RuntimeException(schemaAndColumn + "'s data type has " + sourceClass.getSimpleName()
                                + ", but relation " + oneSchema + "'s data type has" + targetClass.getSimpleName());
                    }
                    // 用列名, 不是别名
                    String[] arr = schemaAndColumn.split("\\.");
                    String relationSchema = arr[0];
                    String relationColumn = arr[1];
                    relationList.add(new SchemaColumnRelation(schema.getName(), column.getName(),
                            relationType, relationSchema, relationColumn));
                }
            }
        }
        return new SchemaColumnInfo(aliasMap, schemaMap, relationList);
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

    public static Class<?> mappingClass(String dbType) {
        String type = (dbType.contains("(") ? dbType.substring(0, dbType.indexOf("(")) : dbType).toLowerCase();
        for (Map.Entry<String, Class<?>> entry : QueryConst.DB_TYPE_MAP.entrySet()) {
            if (type.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        throw new RuntimeException("unknown db type" + dbType);
    }

    public static String toStr(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }

    public static long toLong(Object obj, long defaultLong) {
        if (obj == null) {
            return defaultLong;
        }
        if (obj instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(obj.toString().trim());
        } catch (NumberFormatException e) {
            return defaultLong;
        }
    }

    public static boolean isNumber(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Number) {
            return true;
        }
        try {
            Double.parseDouble(obj.toString().trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** 不是数字则返回 true */
    public static boolean isNotNumber(Object obj) {
        return !isNumber(obj);
    }

    public static <T> T defaultIfNull(T obj, T defaultObj) {
        return (obj == null) ? defaultObj : obj;
    }

    public static String defaultIfBlank(String str1, String defaultStr) {
        return (str1 == null || str1.isEmpty()) ? defaultStr : str1;
    }

    public static boolean isNullString(Object value) {
        if (value instanceof String) {
            String str = ((String) value).trim();
            return "null".equalsIgnoreCase(str) || "undefined".equalsIgnoreCase(str);
        }
        return false;
    }

    public static boolean isNotNullString(Object value) {
        return !isNullString(value);
    }

    public static <T> T first(Collection<T> list) {
        return (list == null || list.isEmpty()) ? null : list.iterator().next();
    }


    public static String getSchemaName(String column, String mainSchema) {
        return column.contains(".") ? column.split("\\.")[0].trim() : mainSchema;
    }

    public static String getColumnName(String column) {
        return column.contains(".") ? column.split("\\.")[1].trim() : column.trim();
    }

    public static SchemaColumn checkColumnName(String column, String mainSchema,
                                               SchemaColumnInfo schemaColumnInfo, String type) {
        String schemaName = getSchemaName(column, mainSchema);
        if (schemaName == null || schemaName.isEmpty()) {
            throw new RuntimeException("schema can't be blank with: " + type);
        }

        Schema schema = schemaColumnInfo.findSchema(schemaName);
        if (schema == null) {
            throw new RuntimeException("no schema(" + schemaName + ") defined with: " + type);
        }

        String columnName = getColumnName(column);
        if (columnName.isEmpty()) {
            throw new RuntimeException("schema(" + schemaName + ") column cant' be blank with: " + type);
        }

        SchemaColumn schemaColumn = schemaColumnInfo.findSchemaColumn(schema, columnName);
        if (schemaColumn == null) {
            throw new RuntimeException("schema(" + schemaName + ") no column(" + columnName + ") defined with: " + type);
        }
        return schemaColumn;
    }

    public static String getRealColumn(boolean needAlias, String column, String mainSchema, SchemaColumnInfo schemaColumnInfo) {
        String schemaName = getSchemaName(column, mainSchema);
        String columnName = getColumnName(column);
        Schema schema = schemaColumnInfo.findSchema(schemaName);
        SchemaColumn schemaColumn = schemaColumnInfo.findSchemaColumn(schema, columnName);
        String useColumnName = QuerySqlUtil.toSqlField(schemaColumn.getName());
        return needAlias ? (QuerySqlUtil.toSqlField(schema.getAlias()) + "." + useColumnName) : useColumnName;
    }
}
