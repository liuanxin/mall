package com.github.global.query.util;

import com.github.global.query.annotation.ColumnInfo;
import com.github.global.query.annotation.TableInfo;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.enums.ReqParamConditionType;
import com.github.global.query.enums.TableRelationType;
import com.github.global.query.model.Table;
import com.github.global.query.model.TableColumn;
import com.github.global.query.model.TableColumnInfo;
import com.github.global.query.model.TableColumnRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueryUtil {

    private static final Logger LOG = LoggerFactory.getLogger(QueryUtil.class);

    private static final PathMatchingResourcePatternResolver RESOLVER =
            new PathMatchingResourcePatternResolver(ClassLoader.getSystemClassLoader());

    private static final MetadataReaderFactory READER = new CachingMetadataReaderFactory(RESOLVER);
    private static final Map<String, Map<String, Field>> FIELDS_CACHE = new ConcurrentHashMap<>();


    public static TableColumnInfo scanTable(String classPackages) {
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
        Map<String, Table> tableMap = new LinkedHashMap<>();
        List<TableColumnRelation> relationList = new ArrayList<>();

        Map<String, ColumnInfo> columnInfoMap = new LinkedHashMap<>();
        Map<String, Class<?>> columnClassMap = new HashMap<>();
        Set<String> tableNameSet = new HashSet<>();
        Set<String> tableAliasSet = new HashSet<>();
        Set<String> columnNameSet = new HashSet<>();
        Set<String> columnAliasSet = new HashSet<>();
        for (Class<?> clazz : classes) {
            TableInfo tableInfo = clazz.getAnnotation(TableInfo.class);
            String tableName, tableDesc, tableAlias;
            if (tableInfo != null) {
                if (tableInfo.ignore()) {
                    continue;
                }

                tableName = tableInfo.value();
                tableDesc = tableInfo.desc();
                tableAlias = defaultIfBlank(tableInfo.alias(), tableName);
            } else {
                tableDesc = "";
                tableAlias = clazz.getSimpleName();
                tableName = aliasToTableName(tableAlias);
            }

            if (tableNameSet.contains(tableName)) {
                throw new RuntimeException("table(" + tableName + ") has renamed");
            }
            tableNameSet.add(tableName);
            if (tableAliasSet.contains(tableAlias)) {
                throw new RuntimeException("table alias(" + tableName + ") has renamed");
            }
            tableAliasSet.add(tableAlias);

            Map<String, TableColumn> columnMap = new LinkedHashMap<>();
            for (Field field : getFields(clazz)) {
                ColumnInfo columnInfo = field.getAnnotation(ColumnInfo.class);
                Class<?> type = field.getType();
                String columnName, columnDesc, columnAlias;
                boolean primary;
                int strLen;
                if (columnInfo != null) {
                    if (columnInfo.ignore()) {
                        continue;
                    }

                    columnName = columnInfo.value();
                    columnDesc = columnInfo.desc();
                    columnAlias = defaultIfBlank(columnInfo.alias(), columnName);
                    primary = columnInfo.primary();
                    strLen = columnInfo.varcharLength();

                    // 用类名 + 列名
                    String tableAndColumn = tableName + "." + columnName;
                    columnInfoMap.put(tableAndColumn, columnInfo);
                    columnClassMap.put(tableAndColumn, type);
                } else {
                    columnDesc = "";
                    columnAlias = field.getName();
                    columnName = aliasToColumnName(columnAlias);
                    primary = "id".equalsIgnoreCase(columnAlias);
                    strLen = 0;
                }

                if (columnNameSet.contains(columnName)) {
                    throw new RuntimeException("table(" + tableAlias + ") has same column(" + columnName + ")");
                }
                columnNameSet.add(columnName);
                if (columnAliasSet.contains(columnAlias)) {
                    throw new RuntimeException("table(" + tableAlias + ") has same column(" + columnAlias + ")");
                }
                columnAliasSet.add(columnAlias);

                aliasMap.put(QueryConst.COLUMN_PREFIX + columnAlias, columnName);
                columnMap.put(columnName, new TableColumn(columnName, columnDesc, columnAlias, primary, strLen, type));
            }
            aliasMap.put(QueryConst.SCHEMA_PREFIX + tableAlias, tableName);
            tableMap.put(tableName, new Table(tableName, tableDesc, tableAlias, columnMap));
        }

        for (Map.Entry<String, ColumnInfo> entry : columnInfoMap.entrySet()) {
            ColumnInfo columnInfo = entry.getValue();
            TableRelationType relationType = columnInfo.relationType();
            if (relationType != TableRelationType.NULL) {
                String oneTable = columnInfo.relationTable();
                String oneColumn = columnInfo.relationColumn();
                if (!oneTable.isEmpty() && !oneColumn.isEmpty()) {
                    String tableAndColumn = entry.getKey();
                    Table table = tableMap.get(aliasMap.get(QueryConst.SCHEMA_PREFIX + oneTable));
                    if (table == null) {
                        table = tableMap.get(oneTable);
                        if (table == null) {
                            throw new RuntimeException(tableAndColumn + "'s relation no table(" + oneTable + ")");
                        }
                    }

                    Map<String, TableColumn> columnMap = table.getColumnMap();
                    TableColumn column = columnMap.get(aliasMap.get(QueryConst.COLUMN_PREFIX + oneColumn));
                    if (column == null) {
                        column = columnMap.get(oneColumn);
                        if (column == null) {
                            throw new RuntimeException(tableAndColumn + "'s relation no table-column("
                                    + oneTable + "." + oneColumn + ")");
                        }
                    }
                    Class<?> sourceClass = columnClassMap.get(tableAndColumn);
                    Class<?> targetClass = column.getColumnType();
                    if (sourceClass != targetClass) {
                        throw new RuntimeException(tableAndColumn + "'s data type has " + sourceClass.getSimpleName()
                                + ", but relation " + oneTable + "'s data type has" + targetClass.getSimpleName());
                    }
                    // 用列名, 不是别名
                    String[] arr = tableAndColumn.split("\\.");
                    String relationTable = arr[0];
                    String relationColumn = arr[1];
                    relationList.add(new TableColumnRelation(table.getName(), column.getName(),
                            relationType, relationTable, relationColumn));
                }
            }
        }
        return new TableColumnInfo(aliasMap, tableMap, relationList);
    }
    /** UserInfo --> user_info */
    private static String aliasToTableName(String className) {
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
    public static String tableNameToAlias(String tableName) {
        if (tableName.toLowerCase().startsWith("t_")) {
            tableName = tableName.substring(2);
        }
        StringBuilder sbd = new StringBuilder();
        char[] chars = tableName.toCharArray();
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

    public static List<Field> getFields(Object obj) {
        return new ArrayList<>(getFields(obj, 0).values());
    }
    private static Map<String, Field> getFields(Object obj, int depth) {
        if (obj == null) {
            return Collections.emptyMap();
        }

        // noinspection DuplicatedCode
        Class<?> clazz = (obj instanceof Class) ? ((Class<?>) obj) : obj.getClass();
        if (clazz == Object.class) {
            return Collections.emptyMap();
        }

        String key = clazz.getName();
        Map<String, Field> fieldCacheMap = FIELDS_CACHE.get(key);
        if (fieldCacheMap != null) {
            return fieldCacheMap;
        }

        Map<String, Field> returnMap = new LinkedHashMap<>();
        Field[] declaredFields = clazz.getDeclaredFields();
        if (declaredFields.length > 0) {
            for (Field declaredField : declaredFields) {
                returnMap.put(declaredField.getName(), declaredField);
            }
        }
        Field[] fields = clazz.getFields();
        if (fields.length > 0) {
            for (Field field : fields) {
                returnMap.put(field.getName(), field);
            }
        }

        Class<?> superclass = clazz.getSuperclass();
        if (superclass != Object.class && depth <= 10) {
            Map<String, Field> fieldMap = getFields(superclass, depth + 1);
            if (!fieldMap.isEmpty()) {
                returnMap.putAll(fieldMap);
            }
        }
        FIELDS_CACHE.put(key, returnMap);
        return returnMap;
    }

    public static Class<?> mappingClass(String dbType) {
        String type = dbType.toLowerCase();
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

    public static int toInt(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Integer toInteger(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Long toLonger(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(obj.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static BigDecimal toDecimal(Object obj) {
        try {
            return new BigDecimal(obj.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static Date toDate(Object obj) {
        for (String format : QueryConst.DATE_FORMAT_LIST) {
            try {
                Date date = new SimpleDateFormat(format).parse(obj.toString().trim());
                if (date != null) {
                    return date;
                }
            } catch (ParseException ignore) {
            }
        }
        return null;
    }

    public static String formatDate(Date date, String pattern, String timezone) {
        if (date == null) {
            return null;
        }
        try {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            if (timezone != null && !timezone.trim().isEmpty()) {
                df.setTimeZone(TimeZone.getTimeZone(timezone.trim()));
            }
            return df.format(date);
        } catch (Exception e) {
            return formatDate(date);
        }
    }
    public static String formatDate(Date date) {
        return new SimpleDateFormat(QueryConst.DEFAULT_DATE_FORMAT).format(date);
    }

    public static boolean isBoolean(Object obj) {
        return obj != null && new HashSet<>(Arrays.asList(
                "true", "1", "on", "yes",
                "false", "0", "off", "no"
        )).contains(obj.toString().toLowerCase());
    }

    public static boolean isLong(Object obj) {
        if (obj == null) {
            return false;
        }

        try {
            Long.parseLong(obj.toString().trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public static boolean isNotLong(Object obj) {
        return !isLong(obj);
    }

    public static boolean isDouble(Object obj) {
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
    public static boolean isNotDouble(Object obj) {
        return !isDouble(obj);
    }

    public static Object toValue(Class<?> type, Object value) {
        if (QueryConst.BOOLEAN_TYPE_SET.contains(type)) {
            return isBoolean(value);
        } else if (QueryConst.INT_TYPE_SET.contains(type)) {
            return toInteger(value);
        } else if (QueryConst.LONG_TYPE_SET.contains(type)) {
            return toLonger(value);
        } else if (Number.class.isAssignableFrom(type)) {
            return toDecimal(value);
        } else if (Date.class.isAssignableFrom(type)) {
            return toDate(value);
        } else {
            return value;
        }
    }

    public static void checkParamType(Class<?> type, ReqParamConditionType conditionType) {
        if (Number.class.isAssignableFrom(type)) {
            if (!QueryConst.NUMBER_TYPE_SET.contains(conditionType)) {
                throw new RuntimeException(QueryConst.NUMBER_TYPE_INFO);
            }
        } else if (Date.class.isAssignableFrom(type)) {
            if (!QueryConst.DATE_TYPE_SET.contains(conditionType)) {
                throw new RuntimeException(QueryConst.DATE_TYPE_INFO);
            }
        } else if (String.class.isAssignableFrom(type)) {
            if (!QueryConst.STRING_TYPE_SET.contains(conditionType)) {
                throw new RuntimeException(QueryConst.STRING_TYPE_INFO);
            }
        } else {
            if (!QueryConst.OTHER_TYPE_SET.contains(conditionType)) {
                throw new RuntimeException(QueryConst.OTHER_TYPE_INFO);
            }
        }
    }

    public static String defaultIfBlank(String str1, String defaultStr) {
        return (str1 == null || str1.isEmpty()) ? defaultStr : str1;
    }

    public static <T> T first(Collection<T> list) {
        return (list == null || list.isEmpty()) ? null : list.iterator().next();
    }


    public static String getTableName(String column, String mainTable) {
        return column.contains(".") ? column.split("\\.")[0].trim() : mainTable;
    }

    public static String getColumnName(String column) {
        return column.contains(".") ? column.split("\\.")[1].trim() : column.trim();
    }

    public static String getUseColumn(boolean needAlias, String column, String mainTable, TableColumnInfo tcInfo) {
        String tableName = getTableName(column, mainTable);
        String columnName = getColumnName(column);
        Table table = tcInfo.findTable(tableName);
        TableColumn tableColumn = tcInfo.findTableColumn(table, columnName);
        String useColumnName = QuerySqlUtil.toSqlField(tableColumn.getName());
        if (needAlias) {
            String alias = table.getAlias();
            return QuerySqlUtil.toSqlField(alias) + "." + useColumnName + " AS " + alias + "_" + tableColumn.getName();
        } else {
            return useColumnName;
        }
    }

    public static String getUseQueryColumn(boolean needAlias, String column, String mainTable, TableColumnInfo tcInfo) {
        String tableName = getTableName(column, mainTable);
        String columnName = getColumnName(column);
        Table table = tcInfo.findTable(tableName);
        TableColumn tableColumn = tcInfo.findTableColumn(table, columnName);
        String tableColumnName = tableColumn.getName();
        String tableColumnAlias = tableColumn.getAlias();
        String useColumnName = QuerySqlUtil.toSqlField(tableColumnName);
        if (needAlias) {
            String alias = table.getAlias();
            return QuerySqlUtil.toSqlField(alias) + "." + useColumnName + " AS " + alias + "_" + tableColumnAlias;
        } else {
            return useColumnName + (tableColumnName.equals(tableColumnAlias) ? "" : (" AS " + tableColumnAlias));
        }
    }
}
