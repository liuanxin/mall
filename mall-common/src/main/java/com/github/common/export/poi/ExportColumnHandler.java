package com.github.common.export.poi;

import com.github.common.util.Arr;
import com.github.common.util.Obj;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

public class ExportColumnHandler {

    /** 从类上收集导出的标题(在字段上标的 &#064;ExportColumn 注解) */
    public static LinkedHashMap<String, String> collectTitle(Class<?> clazz) {
        LinkedHashMap<String, String> titleMap = new LinkedHashMap<>();
        if (Obj.isNotNull(clazz)) {
            Field[] fields = clazz.getDeclaredFields();
            if (Arr.isNotEmpty(fields)) {
                for (Field field : fields) {
                    String name = field.getName();
                    String value = field.getName();
                    ExportColumn column = field.getAnnotation(ExportColumn.class);
                    if (Obj.isNotNull(column)) {
                        String columnValue = column.value();
                        if (Obj.isNotBlank(columnValue)) {
                            value = columnValue;
                        }
                    }
                    titleMap.put(name, value);
                }
            }
        }
        return titleMap;
    }
}
