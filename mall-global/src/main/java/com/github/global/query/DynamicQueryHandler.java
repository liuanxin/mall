package com.github.global.query;

import com.github.common.util.LogUtil;
import com.github.global.query.annotation.SchemeInfo;
import com.github.global.query.model.Scheme;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.StringUtils;

import java.util.*;

public class DynamicQueryHandler {

    private static final PathMatchingResourcePatternResolver RESOLVER =
            new PathMatchingResourcePatternResolver(ClassLoader.getSystemClassLoader());

    private static final MetadataReaderFactory READER = new CachingMetadataReaderFactory(RESOLVER);

    public static Set<Class<?>> scanPackage(String classPackage) {
        if (classPackage == null || classPackage.isEmpty()) {
            return Collections.emptySet();
        }
        String[] paths = StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(classPackage));
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
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("get({}) class exception", path, e);
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
                schemeName = schemeInfo.value();
                schemeDesc = schemeInfo.desc();
                schemeAlias = schemeInfo.alias();
            } else {
                schemeName = schemeDesc = schemeAlias = clazz.getSimpleName();
            }
            if (returnMap.containsKey(schemeName)) {
                throw new RuntimeException("存在同名表名(" + schemeName + ")");
            }
        }
        return returnMap;
    }
}
