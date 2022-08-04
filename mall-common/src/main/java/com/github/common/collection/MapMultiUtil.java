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


    /** 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空), 如果同样的 key 有多个值, 后面将覆盖前面 */
    public static <K, V> Map<K, V> listToMap(Collection<V> list, Function<? super V, K> func) {
        return listToMap(new HashMap<>(), list, func, MapValueDuplicateType.COVER);
    }
    /** 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空) */
    public static <K, V> Map<K, V> listToMap(Collection<V> list, Function<? super V, K> func, MapValueDuplicateType duplicateType) {
        return listToMap(new HashMap<>(), list, func, duplicateType);
    }
    private static <K, V> Map<K, V> listToMap(Map<K, V> returnMap, Collection<V> list,
                                              Function<? super V, K> func, MapValueDuplicateType duplicateType) {
        if (A.isNotEmpty(list)) {
            for (V v : list) {
                if (U.isNotNull(v)) {
                    K k = func.apply(v);
                    if (U.isNotNull(k)) {
                        if (returnMap.containsKey(k)) {
                            duplicateType.handle(returnMap, list, k, v);
                        } else {
                            returnMap.put(k, v);
                        }
                    }
                }
            }
        }
        return returnMap;
    }
    /** 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 如果同样的 key 有多个值, 后面将覆盖前面 */
    public static <K, V> Map<K, V> listToLinkedMap(Collection<V> list, Function<? super V, K> func) {
        return listToMap(new LinkedHashMap<>(), list, func, MapValueDuplicateType.COVER);
    }
    /** 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空) */
    public static <K, V> Map<K, V> listToLinkedMap(Collection<V> list, Function<? super V, K> func,
                                                   MapValueDuplicateType duplicateType) {
        return listToMap(new LinkedHashMap<>(), list, func, duplicateType);
    }


    /** 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空), 如果同样的 key 有多个值, 后面将覆盖前面 */
    public static <T, K, V> Map<K, V> listToMapKeyValue(Collection<T> list, Function<? super T, K> keyFun,
                                                        Function<? super T, V> valueFun) {
        return listToMapKeyValue(new HashMap<>(), list, keyFun, valueFun, MapValueDuplicateType.COVER);
    }
    /** 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空) */
    public static <T, K, V> Map<K, V> listToMapKeyValue(Collection<T> list, Function<? super T, K> keyFun,
                                                        Function<? super T, V> valueFun,
                                                        MapValueDuplicateType duplicateType) {
        return listToMapKeyValue(new HashMap<>(), list, keyFun, valueFun, duplicateType);
    }
    private static <T, K, V> Map<K, V> listToMapKeyValue(Map<K, V> returnMap, Collection<T> list,
                                                         Function<? super T, K> keyFun, Function<? super T, V> valueFun,
                                                         MapValueDuplicateType duplicateType) {
        if (A.isNotEmpty(list)) {
            for (T obj : list) {
                if (U.isNotNull(obj)) {
                    K k = keyFun.apply(obj);
                    if (U.isNotNull(k)) {
                        V v = valueFun.apply(obj);
                        if (U.isNotNull(v)) {
                            if (returnMap.containsKey(k)) {
                                duplicateType.handle(returnMap, list, k, v);
                            } else {
                                returnMap.put(k, v);
                            }
                        }
                    }
                }
            }
        }
        return returnMap;
    }
    /** 用两个指定方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 如果同样的 key 有多个值, 后面将覆盖前面 */
    public static <T, K, V> Map<K, V> listToLinkedMapKeyValue(Collection<T> list,
                                                              Function<? super T, K> keyFun,
                                                              Function<? super T, V> valueFun) {
        return listToMapKeyValue(new LinkedHashMap<>(), list, keyFun, valueFun, MapValueDuplicateType.COVER);
    }
    /** 用两个指定方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空) */
    public static <T, K, V> Map<K, V> listToLinkedMapKeyValue(Collection<T> list,
                                                              Function<? super T, K> keyFun,
                                                              Function<? super T, V> valueFun,
                                                              MapValueDuplicateType duplicateType) {
        return listToMapKeyValue(new LinkedHashMap<>(), list, keyFun, valueFun, duplicateType);
    }


    /** 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 List */
    public static <K, V> Map<K, List<V>> listToMapList(Collection<V> list, Function<? super V, K> func) {
        return listToMapList(list, func, new HashMap<>());
    }
    private static <K, V> Map<K, List<V>> listToMapList(Collection<V> list, Function<? super V, K> func,
                                                        Map<K, List<V>> returnMap) {
        if (A.isNotEmpty(list)) {
            for (V obj : list) {
                if (U.isNotNull(obj)) {
                    K k = func.apply(obj);
                    if (U.isNotNull(k)) {
                        returnMap.computeIfAbsent(k, k1 -> new ArrayList<>()).add(obj);
                    }
                }
            }
        }
        return returnMap;
    }
    /** 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 List */
    public static <K, V> Map<K, List<V>> listToLinkedMapList(Collection<V> list, Function<? super V, K> func) {
        return listToMapList(list, func, new LinkedHashMap<>());
    }


    /** 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 LinkedHashSet */
    public static <K, V> Map<K, Set<V>> listToMapSet(Collection<V> list, Function<? super V, K> func) {
        return listToMapSet(list, func, new HashMap<>());
    }
    private static <K, V> Map<K, Set<V>> listToMapSet(Collection<V> list, Function<? super V, K> func,
                                                      Map<K, Set<V>> returnMap) {
        if (A.isNotEmpty(list)) {
            for (V obj : list) {
                if (U.isNotNull(obj)) {
                    K k = func.apply(obj);
                    if (U.isNotNull(k)) {
                        returnMap.computeIfAbsent(k, k1 -> new LinkedHashSet<>()).add(obj);
                    }
                }
            }
        }
        return returnMap;
    }
    /** 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 LinkedHashSet */
    public static <K, V> Map<K, Set<V>> listToLinkedMapSet(Collection<V> list, Function<? super V, K> func) {
        return listToMapSet(list, func, new LinkedHashMap<>());
    }


    /** 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 List */
    public static <T, K, V> Map<K, List<V>> listToMapKeyValueList(Collection<T> list,
                                                                  Function<? super T, K> keyFun,
                                                                  Function<? super T, V> valueFun) {
        return listToMapKeyValueList(list, keyFun, valueFun, new HashMap<>());
    }
    private static <T, K, V> Map<K, List<V>> listToMapKeyValueList(Collection<T> list,
                                                                   Function<? super T, K> keyFun,
                                                                   Function<? super T, V> valueFun,
                                                                   Map<K, List<V>> returnMap) {
        if (A.isNotEmpty(list)) {
            for (T obj : list) {
                if (U.isNotNull(obj)) {
                    K k = keyFun.apply(obj);
                    if (U.isNotNull(k)) {
                        V v = valueFun.apply(obj);
                        if (U.isNotNull(v)) {
                            returnMap.computeIfAbsent(k, k1 -> new ArrayList<>()).add(v);
                        }
                    }
                }
            }
        }
        return returnMap;
    }
    /** 用两个指定方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 List */
    public static <T, K, V> Map<K, List<V>> listToLinkedMapKeyValueList(Collection<T> list,
                                                                        Function<? super T, K> keyFun,
                                                                        Function<? super T, V> valueFun) {
        return listToMapKeyValueList(list, keyFun, valueFun, new LinkedHashMap<>());
    }


    /** 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是 LinkedHashSet */
    public static <T, K, V> Map<K, Set<V>> listToMapKeyValueSet(Collection<T> list,
                                                                Function<? super T, K> keyFun,
                                                                Function<? super T, V> valueFun) {
        return listToMapKeyValueSet(list, keyFun, valueFun, new HashMap<>());
    }
    private static <T, K, V> Map<K, Set<V>> listToMapKeyValueSet(Collection<T> list,
                                                                 Function<? super T, K> keyFun,
                                                                 Function<? super T, V> valueFun,
                                                                 Map<K, Set<V>> returnMap) {
        if (A.isNotEmpty(list)) {
            for (T obj : list) {
                if (U.isNotNull(obj)) {
                    K k = keyFun.apply(obj);
                    if (U.isNotNull(k)) {
                        V v = valueFun.apply(obj);
                        if (U.isNotNull(v)) {
                            returnMap.computeIfAbsent(k, k1 -> new LinkedHashSet<>()).add(v);
                        }
                    }
                }
            }
        }
        return returnMap;
    }
    /** 用两个指定方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是 LinkedHashSet */
    public static <T, K, V> Map<K, Set<V>> listToLinkedMapKeyValueSet(Collection<T> list,
                                                                      Function<? super T, K> keyFun,
                                                                      Function<? super T, V> valueFun) {
        return listToMapKeyValueSet(list, keyFun, valueFun, new LinkedHashMap<>());
    }
}
