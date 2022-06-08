package com.github.common.bean;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.common.date.DateUtil;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class BeanChange {

    private static final String NEW = "新增";
    private static final String UPDATE_ADD = "增加[%s]新值(%s)";
    private static final String UPDATE = "修改[%s]原值(%s)新值(%s)";
    private static final String UPDATE_DEL = "删除[%s]原值(%s)";
    private static final String DEL = "删除";

    private static final TypeReference<Map<String, String>> MAP_REFERENCE = new TypeReference<>() {};

    private static final Cache<String, Method> METHOD_CACHE_MAP = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();

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

        List<ChangeData> fieldList = new ArrayList<>();
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
                typeSet = new HashSet<>(Arrays.asList(changeProperty.collectGroup()));
            }

            if (collectType == CollectGroup.ALL || typeSet.contains(CollectGroup.ALL) || typeSet.contains(collectType)) {
                Object oldValue = getField(fieldName, clazz, oldObj);
                Object newValue = getField(fieldName, clazz, newObj);
                if (oldValue != newValue) {
                    String value = compareValue(getValue(oldValue, dateFormat), getValue(newValue, dateFormat), name, map);
                    if (U.isNotBlank(value)) {
                        fieldList.add(new ChangeData(order, value));
                    }
                }
            }
        }
        if (A.isNotEmpty(fieldList)) {
            Collections.sort(fieldList);
            List<String> values = new ArrayList<>();
            for (ChangeData cd : fieldList) {
                values.add(cd.value());
            }
            return Joiner.on("; ").join(values).trim();
        }
        return null;
    }

    private static Object getField(String fieldName, Class<?> clazz, Object obj) {
        if (U.isNull(obj)) {
            return null;
        }
        String key = fieldName + "-" + clazz.getName();
        Method method = METHOD_CACHE_MAP.getIfPresent(key);
        if (U.isNull(method)) {
            try {
                method = new PropertyDescriptor(fieldName, clazz).getReadMethod();
            } catch (IntrospectionException e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("call({}) get-field({}) exception", clazz.getName(), fieldName, e);
                }
            }
            if (U.isNotNull(method)) {
                METHOD_CACHE_MAP.put(key, method);
            }
        }
        if (U.isNull(method)) {
            return null;
        }
        try {
            return method.invoke(obj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("call({}) get-field({}) exception", clazz.getName(), fieldName, e);
            }
            return null;
        }
    }

    private static String getValue(Object obj, String dateFormat) {
        if (U.isNull(obj)) {
            return null;
        } else if (obj instanceof Date d) {
            if (U.isBlank(dateFormat)) {
                return DateUtil.formatDateTime(d);
            } else {
                return DateUtil.format(d, dateFormat);
            }
        } else {
            return U.toStr(obj);
        }
    }

    private static String getMapping(Map<String, String> map, String str) {
        String value = map.get(str);
        if (U.isNotBlank(value)) {
            return value;
        }
        String other = map.get("OTHER");
        if (U.isNotBlank(other)) {
            return other;
        }
        return str;
    }

    private static String compareValue(String oldStr, String newStr, String name, Map<String, String> map) {
        if (U.isNull(oldStr) && U.isNull(newStr)) {
            return null;
        } else if (U.isNull(oldStr)) {
            return String.format(UPDATE_ADD, name, getMapping(map, newStr));
        } else if (U.isNull(newStr)) {
            return String.format(UPDATE_DEL, name, getMapping(map, oldStr));
        } else if (!oldStr.equals(newStr)) {
            return String.format(UPDATE, name, getMapping(map, oldStr), getMapping(map, newStr));
        } else {
            return null;
        }
    }
}
