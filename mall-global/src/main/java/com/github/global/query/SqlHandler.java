package com.github.global.query;

import com.github.common.resource.LoaderClass;
import com.github.common.util.U;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class SqlHandler {

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

    public static List<Map<String, Object>> scanPackage(Class clazz, String classPackage) {
        if (U.isNull(clazz) || U.isBlank(classPackage)) {
            return Collections.emptyList();
        }

        List<Class> classes = LoaderClass.getClassList(clazz, classPackage);
        return Collections.emptyList();
    }
}
