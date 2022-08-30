package com.github.global.query;

import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DynamicQueryHandler {

    private static final PathMatchingResourcePatternResolver PATTERN_RESOLVER
            = new PathMatchingResourcePatternResolver(ClassLoader.getSystemClassLoader());

    private static final MetadataReaderFactory READER_FACTORY = new CachingMetadataReaderFactory(PATTERN_RESOLVER);

    /*
    global:
        is null
        is not null
        = (等于)
        <>

    list:
        in (批量)
        not in

    number/date:
        >
        >=
        <
        <=
        between

    string:
        like (开头、结尾、包含), 只有「开头」会走索引(LIKE 'x%'), 结尾是 LIKE '%xx', 包含是 LIKE '%xxx%'
        not like
    */

    public static Set<Class<?>> scanPackage(String classPackage) {
        if (U.isBlank(classPackage)) {
            return Collections.emptySet();
        }
        String[] files = StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(classPackage));
        if (A.isEmpty(files)) {
            return Collections.emptySet();
        }

        Set<Class<?>> classes = new HashSet<>();
        for (String path : files) {
            try {
                Resource[] resources = PATTERN_RESOLVER.getResources(String.format("classpath*:%s*.class", path));
                if (A.isNotEmpty(resources)) {
                    for (Resource resource : resources) {
                        if (resource.isReadable()) {
                            MetadataReader reader = READER_FACTORY.getMetadataReader(resource);
                            String className = reader.getClassMetadata().getClassName();
                            classes.add(Class.forName(className));
                        }
                    }
                }
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("get({}) resource exception", path, e);
                }
            }
        }
        return classes;
    }
}
