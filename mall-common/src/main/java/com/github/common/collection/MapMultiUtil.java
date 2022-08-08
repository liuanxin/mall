package com.github.common.collection;

import com.github.common.util.A;
import com.github.common.util.U;

import java.util.*;
import java.util.function.Function;

public class MapMultiUtil {

    /** 生成 HashMap&lt;K, ArrayList&lt;V&gt;&gt; 实体(value 是数组), 只需要做 add remove, 用 asMap 成最终实体 */
    public static <K, V> MapMultiValue<K, V, List<V>> createMapList() {
        return new MapMultiValue<>(new HashMap<>(), MapValueMultiType.ARRAY_LIST);
    }
    /** 生成 LinkedHashMap&lt;K, ArrayList&lt;V&gt;&gt; 实体(value 是数组), 只需要做 add remove, 用 asMap 成最终实体 */
    public static <K, V> MapMultiValue<K, V, List<V>> createLinkedMapList() {
        return new MapMultiValue<>(new LinkedHashMap<>(), MapValueMultiType.ARRAY_LIST);
    }

    /** 生成 HashMap&lt;K, HashSet&lt;V&gt;&gt; 实体(value 是去重且无序的数组), 只需要做 add remove, 用 asMap 成最终实体 */
    public static <K, V> MapMultiValue<K, V, Set<V>> createMapSet() {
        return new MapMultiValue<>(new HashMap<>(), MapValueMultiType.HASH_SET);
    }
    /** 生成 LinkedHashMap&lt;K, HashSet&lt;V&gt;&gt; 实体(value 是去重且无序的数组), 只需要做 add remove, 用 asMap 成最终实体 */
    public static <K, V> MapMultiValue<K, V, Set<V>> createLinkedMapSet() {
        return new MapMultiValue<>(new LinkedHashMap<>(), MapValueMultiType.HASH_SET);
    }

    /** 生成 HashMap&lt;K, LinkedHashSet&lt;V&gt;&gt; 实体(value 是去重且有序的数组), 只需要做 add remove, 用 asMap 成最终实体 */
    public static <K, V> MapMultiValue<K, V, Set<V>> createMapLinkedSet() {
        return new MapMultiValue<>(new HashMap<>(), MapValueMultiType.LINKED_HASH_SET);
    }
    /** 生成 LinkedHashMap&lt;K, LinkedHashSet&lt;V&gt;&gt; 实体(value 是去重且有序的数组), 只需要做 add remove, 用 asMap 成最终实体 */
    public static <K, V> MapMultiValue<K, V, Set<V>> createLinkedMapLinkedSet() {
        return new MapMultiValue<>(new LinkedHashMap<>(), MapValueMultiType.LINKED_HASH_SET);
    }


    /** 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 List */
    public static <K, V> Map<K, List<V>> listToMapList(Collection<V> list, Function<? super V, K> keyFunc) {
        return listToMapCollection(list, keyFunc, createMapList());
    }
    /** 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 List */
    public static <K, V> Map<K, List<V>> listToLinkedMapList(Collection<V> list, Function<? super V, K> keyFunc) {
        return listToMapCollection(list, keyFunc, createLinkedMapList());
    }

    /** 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 HashSet */
    public static <K, V> Map<K, Set<V>> listToMapSet(Collection<V> list, Function<? super V, K> keyFunc) {
        return listToMapCollection(list, keyFunc, createMapSet());
    }
    /** 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 HashSet */
    public static <K, V> Map<K, Set<V>> listToLinkedMapSet(Collection<V> list, Function<? super V, K> keyFunc) {
        return listToMapCollection(list, keyFunc, createLinkedMapSet());
    }

    /** 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 LinkedHashSet */
    public static <K, V> Map<K, Set<V>> listToMapLinkedSet(Collection<V> list, Function<? super V, K> keyFunc) {
        return listToMapCollection(list, keyFunc, createMapLinkedSet());
    }
    /** 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 LinkedHashSet */
    public static <K, V> Map<K, Set<V>> listToLinkedMapLinkedSet(Collection<V> list, Function<? super V, K> keyFunc) {
        return listToMapCollection(list, keyFunc, createLinkedMapLinkedSet());
    }


    /** 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 List */
    public static <T, K, V> Map<K, List<V>> listToMapKeyValueList(Collection<T> list,
                                                                  Function<? super T, K> keyFunc,
                                                                  Function<? super T, V> valueFunc) {
        return listToMapKeyValueCollection(list, keyFunc, valueFunc, createMapList());
    }
    /** 用两个指定方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 List */
    public static <T, K, V> Map<K, List<V>> listToLinkedMapKeyValueList(Collection<T> list,
                                                                        Function<? super T, K> keyFunc,
                                                                        Function<? super T, V> valueFunc) {
        return listToMapKeyValueCollection(list, keyFunc, valueFunc, createLinkedMapList());
    }

    /** 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 HashSet */
    public static <T, K, V> Map<K, Set<V>> listToMapKeyValueSet(Collection<T> list,
                                                                Function<? super T, K> keyFunc,
                                                                Function<? super T, V> valueFunc) {
        return listToMapKeyValueCollection(list, keyFunc, valueFunc, createMapSet());
    }
    /** 用两个指定方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 HashSet */
    public static <T, K, V> Map<K, Set<V>> listToLinkedMapKeyValueSet(Collection<T> list,
                                                                      Function<? super T, K> keyFunc,
                                                                      Function<? super T, V> valueFunc) {
        return listToMapKeyValueCollection(list, keyFunc, valueFunc, createLinkedMapSet());
    }

    /** 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 LinkedHashSet */
    public static <T, K, V> Map<K, Set<V>> listToMapKeyValueLinkedSet(Collection<T> list,
                                                                      Function<? super T, K> keyFunc,
                                                                      Function<? super T, V> valueFunc) {
        return listToMapKeyValueCollection(list, keyFunc, valueFunc, createMapLinkedSet());
    }
    /** 用两个指定方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 LinkedHashSet */
    public static <T, K, V> Map<K, Set<V>> listToLinkedMapKeyValueLinkedSet(Collection<T> list,
                                                                            Function<? super T, K> keyFunc,
                                                                            Function<? super T, V> valueFunc) {
        return listToMapKeyValueCollection(list, keyFunc, valueFunc, createLinkedMapLinkedSet());
    }

    private static <K, V, C extends Collection<V>> Map<K, C> listToMapCollection(Collection<V> list,
                                                                                 Function<? super V, K> keyFunc,
                                                                                 MapMultiValue<K, V, C> multiValueMap) {
        if (A.isNotEmpty(list)) {
            for (V v : list) {
                if (U.isNotNull(v)) {
                    K k = keyFunc.apply(v);
                    if (U.isNotNull(k)) {
                        multiValueMap.put(k, v);
                    }
                }
            }
        }
        return multiValueMap.asMap();
    }
    private static <T, K, V, C extends Collection<V>> Map<K, C> listToMapKeyValueCollection(Collection<T> list,
                                                                                            Function<? super T, K> keyFunc,
                                                                                            Function<? super T, V> valueFunc,
                                                                                            MapMultiValue<K, V, C> multiValueMap) {
        if (A.isNotEmpty(list)) {
            for (T obj : list) {
                if (U.isNotNull(obj)) {
                    K k = keyFunc.apply(obj);
                    if (U.isNotNull(k)) {
                        V v = valueFunc.apply(obj);
                        if (U.isNotNull(v)) {
                            multiValueMap.put(k, v);
                        }
                    }
                }
            }
        }
        return multiValueMap.asMap();
    }
}
