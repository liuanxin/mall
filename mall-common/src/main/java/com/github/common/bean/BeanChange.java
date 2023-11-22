package com.github.common.bean;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.common.date.DateUtil;
import com.github.common.function.Multi;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.U;

import java.lang.reflect.Field;
import java.util.*;

public final class BeanChange {

    private static final String NEW = "新增";
    private static final String DEL = "删除";

    private static final String INSERT = "增加[%s]新值(%s)";
    private static final String UPDATE = "修改[%s]原值(%s)新值(%s)";
    private static final String REMOVE = "去除[%s]原值(%s)";

    private static final String VALUE_OTHER_KEY = "OTHER";

    private static final TypeReference<Map<String, String>> MAP_REFERENCE = new TypeReference<>() {};


    public static <T> String diff(T oldObj, T newObj) {
        return diff(CollectProperty.Group.ALL, oldObj, newObj);
    }

    public static <T> String diff(CollectProperty.Group collectType, T oldObj, T newObj) {
        if (oldObj == newObj) {
            return null;
        }
        if (U.isNull(oldObj)) {
            return NEW;
        }
        if (U.isNull(newObj)) {
            return DEL;
        }

        List<Multi.Two<Integer, String>> fieldList = new ArrayList<>();
        CollectProperty.Group all = CollectProperty.Group.ALL;
        for (Field field : U.getFields(oldObj)) {
            String fieldName = field.getName();
            CollectProperty cp = field.getAnnotation(CollectProperty.class);
            JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
            String name;
            int order;
            String dateFormat;
            Map<String, String> map;
            Set<CollectProperty.Group> typeSet;
            if (U.isNull(cp)) {
                name = fieldName;
                order = Integer.MAX_VALUE;
                dateFormat = U.isNull(jsonFormat) ? null : jsonFormat.pattern();
                map = Collections.emptyMap();
                typeSet = Collections.singleton(all);
            } else {
                name = cp.value();
                order = cp.order();
                dateFormat = U.isNull(jsonFormat) ? cp.dateFormat() : jsonFormat.pattern();
                Map<String, String> valueMapping = JsonUtil.convertType(cp.valueMapping(), MAP_REFERENCE);
                map = A.isEmpty(valueMapping) ? Collections.emptyMap() : valueMapping;
                typeSet = new HashSet<>(Arrays.asList(cp.group()));
            }

            if (collectType == all || typeSet.contains(all) || typeSet.contains(collectType)) {
                Object oldValue = getField(fieldName, oldObj);
                Object newValue = getField(fieldName, newObj);
                if (U.notEquals(oldValue, newValue)) {
                    String value = compareValue(getValue(oldValue, dateFormat), getValue(newValue, dateFormat), name, map);
                    if (U.isNotBlank(value)) {
                        fieldList.add(new Multi.Two<>(order, value));
                    }
                }
            }
        }
        if (A.isNotEmpty(fieldList)) {
            fieldList.sort(Comparator.comparingInt(Multi.Two::one));
            List<String> values = new ArrayList<>();
            for (Multi.Two<Integer, String> field : fieldList) {
                values.add(U.toStr(field.two()));
            }
            return "<" + String.join(">,<", values) + ">";
        }
        return null;
    }

    private static Object getField(String field, Object obj) {
        return U.invokeMethod(obj, "get" + field.substring(0, 1).toUpperCase() + field.substring(1));
    }

    private static String getValue(Object obj, String dateFormat) {
        if (obj instanceof Date d) {
            return U.isBlank(dateFormat) ? DateUtil.formatDateTime(d) : DateUtil.format(d, dateFormat);
        } else {
            return U.toStr(obj);
        }
    }

    private static String compareValue(String oldStr, String newStr, String name, Map<String, String> map) {
        if (U.isNull(oldStr)) {
            return String.format(INSERT, name, getMapping(map, newStr));
        } else if (U.isNull(newStr)) {
            return String.format(REMOVE, name, getMapping(map, oldStr));
        } else if (!oldStr.equals(newStr)) {
            return String.format(UPDATE, name, getMapping(map, oldStr), getMapping(map, newStr));
        } else {
            return null;
        }
    }

    private static String getMapping(Map<String, String> map, String str) {
        String value = map.get(str);
        if (U.isNotBlank(value)) {
            return value;
        }
        String other = map.get(VALUE_OTHER_KEY);
        if (U.isNotBlank(other)) {
            return other;
        }
        return str;
    }
}
