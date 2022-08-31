package com.github.global.query;

import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class DynamicQueryHandler {

    private static final PathMatchingResourcePatternResolver RESOLVER =
            new PathMatchingResourcePatternResolver(ClassLoader.getSystemClassLoader());

    private static final MetadataReaderFactory READER = new CachingMetadataReaderFactory(RESOLVER);


    public static Set<Class<?>> scanPackage(String classPackage) {
        if (U.isBlank(classPackage)) {
            return Collections.emptySet();
        }
        String[] paths = StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(classPackage));
        if (A.isEmpty(paths)) {
            return Collections.emptySet();
        }

        Set<Class<?>> set = new LinkedHashSet<>();
        if (A.isNotEmpty(paths)) {
            for (String path : paths) {
                try {
                    String location = String.format("classpath*:**/%s/**/*.class", path.replace(".", "/"));
                    Resource[] resources = RESOLVER.getResources(location);
                    if (A.isNotEmpty(resources)) {
                        for (Resource resource : resources) {
                            if (resource.isReadable()) {
                                String className = READER.getMetadataReader(resource).getClassMetadata().getClassName();
                                set.add(Class.forName(className));
                            }
                        }
                    }
                } catch (Exception e) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error("get({}) resource exception", path, e);
                    }
                }
            }
        }
        return set;
    }
}
