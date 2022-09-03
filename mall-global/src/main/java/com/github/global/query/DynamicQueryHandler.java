package com.github.global.query;

import com.github.common.util.U;
import com.github.global.query.annotation.ColumnInfo;
import com.github.global.query.annotation.SchemeInfo;
import com.github.global.query.model.Scheme;
import com.github.global.query.model.SchemeColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;

public class DynamicQueryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicQueryHandler.class);

    private static final PathMatchingResourcePatternResolver RESOLVER =
            new PathMatchingResourcePatternResolver(ClassLoader.getSystemClassLoader());

    private static final MetadataReaderFactory READER = new CachingMetadataReaderFactory(RESOLVER);

    public static Map<String, Scheme> scanScheme(String classPackages) {
        return handleTable(scanPackage(classPackages));
    }

    private static Set<Class<?>> scanPackage(String classPackages) {
        if (classPackages == null || classPackages.isEmpty()) {
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

    private static Map<String, Scheme> handleTable(Set<Class<?>> classes) {
        Map<String, Scheme> returnMap = new LinkedHashMap<>();
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
            if (returnMap.containsKey(schemeAlias)) {
                throw new RuntimeException("存在同名表(" + schemeName + ")");
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
                } else {
                    columnDesc = "";
                    columnAlias = field.getName();
                    columnName = convertColumnName(columnAlias);
                    primary = "id".equalsIgnoreCase(field.getName());
                }
                if (columnMap.containsKey(columnAlias)) {
                    throw new RuntimeException("表(" + schemeName + ")中存在同名属性(" + columnAlias + ")");
                }

                SchemeColumn column = new SchemeColumn(columnName, columnDesc, columnAlias, primary, field.getType());
                columnMap.put(columnAlias, column);
            }
            returnMap.put(schemeAlias, new Scheme(schemeName, schemeDesc, schemeAlias, columnMap));
        }
        return returnMap;
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
}
