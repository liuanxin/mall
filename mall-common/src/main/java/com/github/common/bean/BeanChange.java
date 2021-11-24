package com.github.common.bean;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.common.date.DateUtil;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.U;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.*;

public final class BeanChange {

    private static final String NEW = "新增";
    private static final String UPDATE_ADD = "增加[%s]新值(%s)";
    private static final String UPDATE = "修改[%s]原值(%s)新值(%s)";
    private static final String UPDATE_DEL = "删除[%s]原值(%s)";
    private static final String DEL = "删除";

    private static final TypeReference<Map<String, String>> MAP_REFERENCE = new TypeReference<>() {};

    public static <T> String diff(T oldObj, T newObj) {
        return diff(CollectGroup.ALL, oldObj, newObj);
    }

    public static <T> String diff(CollectGroup collectType, T oldObj, T newObj) {
        if (oldObj == newObj) {
            return null;
        }
        if (U.isNull(oldObj)) {
            return NEW;
        }
        if (U.isNull(newObj)) {
            return DEL;
        }

        Map<Integer, String> fieldMap = Maps.newLinkedHashMap();
        Class<?> clazz = oldObj.getClass();
        for (Field field : U.getAllField(clazz)) {
            String fieldName = field.getName();
            CollectProperty changeProperty = field.getAnnotation(CollectProperty.class);
            String name;
            int order;
            String dateFormat;
            Map<String, String> map;
            Set<CollectGroup> typeSet;
            if (U.isNull(changeProperty)) {
                name = fieldName;
                order = Integer.MAX_VALUE;
                dateFormat = null;
                map = Collections.emptyMap();
                typeSet = Collections.singleton(CollectGroup.ALL);
            } else {
                name = changeProperty.value();
                order = changeProperty.order();
                dateFormat = changeProperty.dateFormat();
                Map<String, String> valueMapping = JsonUtil.convertType(changeProperty.valueMapping(), MAP_REFERENCE);
                map = A.isEmpty(valueMapping) ? Collections.emptyMap() : valueMapping;
                typeSet = Sets.newHashSet(changeProperty.collectGroup());
            }

            if (collectType == CollectGroup.ALL || typeSet.contains(CollectGroup.ALL) || typeSet.contains(collectType)) {
                Object oldValue;
                try {
                    oldValue = new PropertyDescriptor(fieldName, clazz).getReadMethod().invoke(oldObj);
                } catch (Exception ignore) {
                    oldValue = null;
                }

                Object newValue;
                try {
                    newValue = new PropertyDescriptor(fieldName, clazz).getReadMethod().invoke(newObj);
                } catch (Exception ignore) {
                    newValue = null;
                }

                if (oldValue != newValue) {
                    String value = compareValue(getValue(oldValue, dateFormat), getValue(newValue, dateFormat), name, map);
                    if (U.isNotBlank(value)) {
                        fieldMap.put(order, value);
                    }
                }
            }
        }
        if (A.isNotEmpty(fieldMap)) {
            List<Integer> keys = Lists.newArrayList(fieldMap.keySet());
            Collections.sort(keys);
            List<String> values = Lists.newArrayList();
            for (Integer key : keys) {
                values.add(fieldMap.get(key));
            }
            return Joiner.on("; ").join(values).trim();
        }
        return null;
    }

    private static String getValue(Object obj, String dateFormat) {
        if (U.isNull(obj)) {
            return null;
        } else if (obj instanceof Date) {
            if (U.isBlank(dateFormat)) {
                return DateUtil.formatDateTime((Date) obj);
            } else {
                return DateUtil.format((Date) obj, dateFormat);
            }
        } else {
            return U.toStr(obj);
        }
    }

    private static String getMapping(Map<String, String> map, String obj) {
        String value = map.get(obj);
        if (U.isNotBlank(value)) {
            return value;
        }
        String other = map.get("OTHER");
        if (U.isNotBlank(other)) {
            return other;
        }
        return obj;
    }

    private static String compareValue(String oldObj, String newObj, String name, Map<String, String> map) {
        if (U.isNull(oldObj) && U.isNull(newObj)) {
            return null;
        } else if (U.isNull(oldObj)) {
            return String.format(UPDATE_ADD, name, getMapping(map, newObj));
        } else if (U.isNull(newObj)) {
            return String.format(UPDATE_DEL, name, getMapping(map, oldObj));
        } else if (!oldObj.equals(newObj)) {
            return String.format(UPDATE, name, getMapping(map, oldObj), getMapping(map, newObj));
        } else {
            return null;
        }
    }
}
