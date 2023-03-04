package com.github.common.util;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class MapMultiUtil {

    /** 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 List */
    public static <K, V> Map<K, List<V>> listToMapList(Collection<V> list, Function<? super V, K> keyFunc) {
        return listToMapCollection(list, keyFunc, new HashMap<>(), ArrayList::new);
    }
    /** 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 List */
    public static <K, V> Map<K, List<V>> listToLinkedMapList(Collection<V> list, Function<? super V, K> keyFunc) {
        return listToMapCollection(list, keyFunc, new LinkedHashMap<>(), ArrayList::new);
    }

    /** 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 HashSet */
    public static <K, V> Map<K, Set<V>> listToMapSet(Collection<V> list, Function<? super V, K> keyFunc) {
        return listToMapCollection(list, keyFunc, new HashMap<>(), HashSet::new);
    }
    /** 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 HashSet */
    public static <K, V> Map<K, Set<V>> listToLinkedMapSet(Collection<V> list, Function<? super V, K> keyFunc) {
        return listToMapCollection(list, keyFunc, new LinkedHashMap<>(), HashSet::new);
    }

    /** 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 LinkedHashSet */
    public static <K, V> Map<K, Set<V>> listToMapLinkedSet(Collection<V> list, Function<? super V, K> keyFunc) {
        return listToMapCollection(list, keyFunc, new HashMap<>(), LinkedHashSet::new);
    }
    /** 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 LinkedHashSet */
    public static <K, V> Map<K, Set<V>> listToLinkedMapLinkedSet(Collection<V> list, Function<? super V, K> keyFunc) {
        return listToMapCollection(list, keyFunc, new LinkedHashMap<>(), LinkedHashSet::new);
    }


    /** 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 List */
    public static <T, K, V> Map<K, List<V>> listToMapKeyValueList(Collection<T> list,
                                                                  Function<? super T, K> keyFunc,
                                                                  Function<? super T, V> valueFunc) {
        return listToMapKeyValueCollection(list, keyFunc, valueFunc, new HashMap<>(), ArrayList::new);
    }
    /** 用两个指定方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 List */
    public static <T, K, V> Map<K, List<V>> listToLinkedMapKeyValueList(Collection<T> list,
                                                                        Function<? super T, K> keyFunc,
                                                                        Function<? super T, V> valueFunc) {
        return listToMapKeyValueCollection(list, keyFunc, valueFunc, new LinkedHashMap<>(), ArrayList::new);
    }

    /** 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 HashSet */
    public static <T, K, V> Map<K, Set<V>> listToMapKeyValueSet(Collection<T> list,
                                                                Function<? super T, K> keyFunc,
                                                                Function<? super T, V> valueFunc) {
        return listToMapKeyValueCollection(list, keyFunc, valueFunc, new HashMap<>(), HashSet::new);
    }
    /** 用两个指定方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 HashSet */
    public static <T, K, V> Map<K, Set<V>> listToLinkedMapKeyValueSet(Collection<T> list,
                                                                      Function<? super T, K> keyFunc,
                                                                      Function<? super T, V> valueFunc) {
        return listToMapKeyValueCollection(list, keyFunc, valueFunc, new LinkedHashMap<>(), HashSet::new);
    }

    /** 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 LinkedHashSet */
    public static <T, K, V> Map<K, Set<V>> listToMapKeyValueLinkedSet(Collection<T> list,
                                                                      Function<? super T, K> keyFunc,
                                                                      Function<? super T, V> valueFunc) {
        return listToMapKeyValueCollection(list, keyFunc, valueFunc, new HashMap<>(), LinkedHashSet::new);
    }
    /** 用两个指定方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 LinkedHashSet */
    public static <T, K, V> Map<K, Set<V>> listToLinkedMapKeyValueLinkedSet(Collection<T> list,
                                                                            Function<? super T, K> keyFunc,
                                                                            Function<? super T, V> valueFunc) {
        return listToMapKeyValueCollection(list, keyFunc, valueFunc, new LinkedHashMap<>(), LinkedHashSet::new);
    }


    private static <K, V, C extends Collection<V>> Map<K, C> listToMapCollection(Collection<V> list,
                                                                                 Function<? super V, K> keyFunc,
                                                                                 Map<K, C> multiMap,
                                                                                 Supplier<C> instance) {
        if (A.isNotEmpty(list)) {
            for (V v : list) {
                if (U.isNotNull(v)) {
                    K k = keyFunc.apply(v);
                    if (U.isNotNull(k)) {
                        multiMap.computeIfAbsent(k, (k1) -> instance.get()).add(v);
                    }
                }
            }
        }
        return multiMap;
    }
    private static <T, K, V, C extends Collection<V>> Map<K, C> listToMapKeyValueCollection(Collection<T> list,
                                                                                            Function<? super T, K> keyFunc,
                                                                                            Function<? super T, V> valueFunc,
                                                                                            Map<K, C> multiMap,
                                                                                            Supplier<C> instance) {
        if (A.isNotEmpty(list)) {
            for (T obj : list) {
                if (U.isNotNull(obj)) {
                    K k = keyFunc.apply(obj);
                    if (U.isNotNull(k)) {
                        V v = valueFunc.apply(obj);
                        if (U.isNotNull(v)) {
                            multiMap.computeIfAbsent(k, (k1) -> instance.get()).add(v);
                        }
                    }
                }
            }
        }
        return multiMap;
    }
}
