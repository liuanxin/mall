package com.github.common.util;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

/** 集合相关的工具包 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class A {

    private static final String SPLIT = ",";

    public static boolean isArray(Object obj) {
        return obj != null && (obj.getClass().isArray() || obj instanceof Collection);
    }
    public static boolean isNotArray(Object obj) {
        return !isArray(obj);
    }

    public static boolean isEmpty(Object arrayOrCollectionOrMap) {
        if (arrayOrCollectionOrMap == null) {
            return true;
        } else if (arrayOrCollectionOrMap.getClass().isArray()) {
            return Array.getLength(arrayOrCollectionOrMap) == 0;
        } else if (arrayOrCollectionOrMap instanceof Collection) {
            return ((Collection<?>) arrayOrCollectionOrMap).isEmpty();
        } else if (arrayOrCollectionOrMap instanceof Map) {
            return ((Map) arrayOrCollectionOrMap).isEmpty();
        } else {
            return false;
        }
    }
    public static boolean isNotEmpty(Object arrayOrCollectionOrMap) {
        return !isEmpty(arrayOrCollectionOrMap);
    }

    public static <T> boolean isEmpty(T[] array) {
        if (array == null || array.length == 0) {
            return true;
        }
        // 如果每一项都是 null, 也返回 true
        for (T t : array) {
            if (t != null) {
                return false;
            }
        }
        return true;
    }
    public static <T> boolean isNotEmpty(T[] array) {
        return !isEmpty(array);
    }

    public static boolean isEmpty(Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return true;
        }
        // 如果每一项都是 null, 也返回 true
        for (Object t : collection) {
            if (t != null) {
                return false;
            }
        }
        return true;
    }
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isEmpty(Map map) {
        return map == null || map.isEmpty();
    }
    public static boolean isNotEmpty(Map map) {
        return !isEmpty(map);
    }

    public static String toStr(Collection<?> collection) {
        return toStr(collection, SPLIT);
    }
    public static String toStr(Collection<?> collection, String split) {
        if (isEmpty(collection)) {
            return U.EMPTY;
        } else {
            StringBuilder sbd = new StringBuilder();
            int i = 0;
            for (Object obj : collection) {
                if (i > 0) {
                    sbd.append(split);
                }
                sbd.append(obj);
                i++;
            }
            return sbd.toString();
        }
    }
    public static String toString(Object arrayOrCollection) {
        return toString(arrayOrCollection, SPLIT);
    }
    public static String toString(Object arrayOrCollection, String split) {
        if (arrayOrCollection != null) {
            if (arrayOrCollection.getClass().isArray()) {
                StringBuilder sbd = new StringBuilder();
                int len = Array.getLength(arrayOrCollection);
                for (int i = 0; i < len; i++) {
                    if (i > 0) {
                        sbd.append(split);
                    }
                    sbd.append(U.toStr(Array.get(arrayOrCollection, i)));
                }
                return sbd.toString();
            } else if (arrayOrCollection instanceof Collection) {
                return toStr((Collection<?>) arrayOrCollection, split);
            } else {
                return arrayOrCollection.toString();
            }
        } else {
            return U.EMPTY;
        }
    }

    public static String toStr(Object[] array) {
        return toStr(array, SPLIT);
    }
    public static String toStr(Object[] array, String split) {
        if (isEmpty(array)) {
            return U.EMPTY;
        }

        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sbd.append(split);
            }
            sbd.append(array[i]);
        }
        return sbd.toString();
    }

    public static <K, T> Map<K, T> listToMap(Collection<T> list, Function<? super T, K> func) {
        if (isEmpty(list)) {
            return Collections.emptyMap();
        }

        Map<K, T> returnMap = new HashMap<>();
        for (T obj : list) {
            if (U.isNotNull(obj)) {
                K k = func.apply(obj);
                if (U.isNotNull(k)) {
                    returnMap.put(k, obj);
                }
            }
        }
        return returnMap;
    }
    public static <K, T> Map<K, T> listToLinkedMap(Collection<T> list, Function<? super T, K> func) {
        if (isEmpty(list)) {
            return Collections.emptyMap();
        }

        Map<K, T> returnMap = new LinkedHashMap<>();
        for (T obj : list) {
            if (U.isNotNull(obj)) {
                K k = func.apply(obj);
                if (U.isNotNull(k)) {
                    returnMap.put(k, obj);
                }
            }
        }
        return returnMap;
    }

    public static <K, T> Map<K, List<T>> listToMapList(Collection<T> list, Function<? super T, K> func) {
        if (isEmpty(list)) {
            return Collections.emptyMap();
        }

        Map<K, List<T>> returnMap = new HashMap<>();
        for (T obj : list) {
            if (U.isNotNull(obj)) {
                K k = func.apply(obj);
                if (U.isNotNull(k)) {
                    List<T> array = returnMap.get(k);
                    if (U.isNull(array)) {
                        array = new ArrayList<>();
                        returnMap.put(k, array);
                    }
                    array.add(obj);
                }
            }
        }
        return returnMap;
    }
    public static <K, T> Map<K, List<T>> listToLinkedMapList(Collection<T> list, Function<? super T, K> func) {
        if (isEmpty(list)) {
            return Collections.emptyMap();
        }

        Map<K, List<T>> returnMap = new LinkedHashMap<>();
        for (T obj : list) {
            if (U.isNotNull(obj)) {
                K k = func.apply(obj);
                if (U.isNotNull(k)) {
                    List<T> array = returnMap.get(k);
                    if (U.isNull(array)) {
                        array = new ArrayList<>();
                        returnMap.put(k, array);
                    }
                    array.add(obj);
                }
            }
        }
        return returnMap;
    }

    public static <K, T> Map<K, Set<T>> listToMapSet(Collection<T> list, Function<? super T, K> func) {
        if (isEmpty(list)) {
            return Collections.emptyMap();
        }

        Map<K, Set<T>> returnMap = new HashMap<>();
        for (T obj : list) {
            if (U.isNotNull(obj)) {
                K k = func.apply(obj);
                if (U.isNotNull(k)) {
                    Set<T> array = returnMap.get(k);
                    if (isEmpty(array)) {
                        array = new LinkedHashSet<>();
                        returnMap.put(k, array);
                    }
                    array.add(obj);
                }
            }
        }
        return returnMap;
    }
    public static <K, T> Map<K, Set<T>> listToLinkedMapSet(Collection<T> list, Function<? super T, K> func) {
        if (isEmpty(list)) {
            return Collections.emptyMap();
        }

        Map<K, Set<T>> returnMap = new LinkedHashMap<>();
        for (T obj : list) {
            if (U.isNotNull(obj)) {
                K k = func.apply(obj);
                if (U.isNotNull(k)) {
                    Set<T> array = returnMap.get(k);
                    if (isEmpty(array)) {
                        array = new LinkedHashSet<>();
                        returnMap.put(k, array);
                    }
                    array.add(obj);
                }
            }
        }
        return returnMap;
    }

    public static <T, K, V> Map<K, V> listToMapKeyValue(Collection<T> list,
                                                        Function<? super T, K> keyFun,
                                                        Function<? super T, V> valueFun) {
        if (isEmpty(list)) {
            return Collections.emptyMap();
        }

        Map<K, V> returnMap = new HashMap<>();
        for (T obj : list) {
            if (U.isNotNull(obj)) {
                K k = keyFun.apply(obj);
                if (U.isNotNull(k)) {
                    V v = valueFun.apply(obj);
                    if (U.isNotNull(v)) {
                        returnMap.put(k, v);
                    }
                }
            }
        }
        return returnMap;
    }
    public static <T, K, V> Map<K, V> listToLinkedMapKeyValue(Collection<T> list,
                                                              Function<? super T, K> keyFun,
                                                              Function<? super T, V> valueFun) {
        if (isEmpty(list)) {
            return Collections.emptyMap();
        }

        Map<K, V> returnMap = new LinkedHashMap<>();
        for (T obj : list) {
            if (U.isNotNull(obj)) {
                K k = keyFun.apply(obj);
                if (U.isNotNull(k)) {
                    V v = valueFun.apply(obj);
                    if (U.isNotNull(v)) {
                        returnMap.put(k, v);
                    }
                }
            }
        }
        return returnMap;
    }

    public static <T, K, V> Map<K, List<V>> listToMapKeyValueList(Collection<T> list,
                                                                  Function<? super T, K> keyFun,
                                                                  Function<? super T, V> valueFun) {
        if (isEmpty(list)) {
            return Collections.emptyMap();
        }

        Map<K, List<V>> returnMap = new HashMap<>();
        for (T obj : list) {
            if (U.isNotNull(obj)) {
                K k = keyFun.apply(obj);
                if (U.isNotNull(k)) {
                    V v = valueFun.apply(obj);
                    if (U.isNotNull(v)) {
                        List<V> array = returnMap.get(k);
                        if (U.isNull(array)) {
                            array = new ArrayList<>();
                            returnMap.put(k, array);
                        }
                        array.add(v);
                    }
                }
            }
        }
        return returnMap;
    }
    public static <T, K, V> Map<K, List<V>> listToLinkedMapKeyValueList(Collection<T> list,
                                                                        Function<? super T, K> keyFun,
                                                                        Function<? super T, V> valueFun) {
        if (isEmpty(list)) {
            return Collections.emptyMap();
        }

        Map<K, List<V>> returnMap = new LinkedHashMap<>();
        for (T obj : list) {
            if (U.isNotNull(obj)) {
                K k = keyFun.apply(obj);
                if (U.isNotNull(k)) {
                    V v = valueFun.apply(obj);
                    if (U.isNotNull(v)) {
                        List<V> array = returnMap.get(k);
                        if (U.isNull(array)) {
                            array = new ArrayList<>();
                            returnMap.put(k, array);
                        }
                        array.add(v);
                    }
                }
            }
        }
        return returnMap;
    }

    public static <T, K, V> Map<K, Set<V>> listToMapKeyValueSet(Collection<T> list,
                                                                Function<? super T, K> keyFun,
                                                                Function<? super T, V> valueFun) {
        if (isEmpty(list)) {
            return Collections.emptyMap();
        }

        Map<K, Set<V>> returnMap = new HashMap<>();
        for (T obj : list) {
            if (U.isNotNull(obj)) {
                K k = keyFun.apply(obj);
                if (U.isNotNull(k)) {
                    V v = valueFun.apply(obj);
                    if (U.isNotNull(v)) {
                        Set<V> array = returnMap.get(k);
                        if (isEmpty(array)) {
                            array = new LinkedHashSet<>();
                            returnMap.put(k, array);
                        }
                        array.add(v);
                    }
                }
            }
        }
        return returnMap;
    }
    public static <T, K, V> Map<K, Set<V>> listToLinkedMapKeyValueSet(Collection<T> list,
                                                                      Function<? super T, K> keyFun,
                                                                      Function<? super T, V> valueFun) {
        if (isEmpty(list)) {
            return Collections.emptyMap();
        }

        Map<K, Set<V>> returnMap = new LinkedHashMap<>();
        for (T obj : list) {
            if (U.isNotNull(obj)) {
                K k = keyFun.apply(obj);
                if (U.isNotNull(k)) {
                    V v = valueFun.apply(obj);
                    if (U.isNotNull(v)) {
                        Set<V> array = returnMap.get(k);
                        if (isEmpty(array)) {
                            array = new LinkedHashSet<>();
                            returnMap.put(k, array);
                        }
                        array.add(v);
                    }
                }
            }
        }
        return returnMap;
    }

    public static <T, R> List<R> collect(Collection<T> list, Function<T, R> func) {
        if (isEmpty(list)) {
            return Collections.emptyList();
        }

        List<R> returnList = new ArrayList<>();
        for (T obj : list) {
            if (U.isNotNull(obj)) {
                R value = func.apply(obj);
                if (U.isNotNull(value)) {
                    returnList.add(value);
                }
            }
        }
        return returnList;
    }

    public static <T> List<List<T>> split(List<T> list, int singleSize) {
        if (isEmpty(list) || singleSize <= 0) {
            return Collections.emptyList();
        }

        List<List<T>> returnList = new ArrayList<>();
        int size = list.size();
        int loop = (size / singleSize);
        if (size % singleSize > 0) {
            loop += 1;
        }
        for (int i = 0; i < loop; i++) {
            List<T> innerList = new ArrayList<>();
            int j = (i * singleSize);
            int innerLoop = (j + singleSize);
            if (innerLoop > size) {
                innerLoop = size;
            }
            for (; j < innerLoop; j++) {
                innerList.add(list.get(j));
            }
            returnList.add(innerList);
        }
        return returnList;
    }

    /** 数组去重返回 */
    public static <T> Collection<T> removeDuplicate(T[] array) {
        return removeDuplicate(Arrays.asList(array));
    }
    /** 删除重复的项 */
    public static <T> Collection<T> removeDuplicate(Collection<T> array) {
        // ImmutableSet.copyOf(array).asList(); // guava
        return new LinkedHashSet<>(array);
    }

    /** 构造 HashMap, 必须保证每两个参数的类型是一致的! 当参数是奇数时, 最后一个 key 将会被忽略 */
    public static <K, V> HashMap<K, V> maps(Object... keysAndValues) {
        return (HashMap<K, V>) maps(new HashMap<>(), keysAndValues);
    }
    private static <K, V> Map<K, V> maps(Map<K, V> result, Object... keysAndValues) {
        if (isNotEmpty(keysAndValues)) {
            for (int i = 0; i < keysAndValues.length; i += 2) {
                if (keysAndValues.length > (i + 1)) {
                    result.put((K) keysAndValues[i], (V) keysAndValues[i + 1]);
                }
            }
        }
        return result;
    }
    /** 构造 LinkedHashMap, 必须保证每两个参数的类型是一致的! 当参数是奇数时, 最后一个 key 将会被忽略 */
    public static <K, V> LinkedHashMap<K, V> linkedMaps(Object... keysAndValues) {
        return (LinkedHashMap<K, V>) maps(new LinkedHashMap<>(), keysAndValues);
    }

    /** 获取数组的第一个元素 */
    public static <T> T first(T[] array) {
        return isEmpty(array) ? null : array[0];
    }
    /** 获取数组的最后一个元素 */
    public static <T> T last(T[] array) {
        return isEmpty(array) ? null : array[array.length - 1];
    }

    /** 获取集合的第一个元素 */
    public static <T> T first(Collection<T> collection) {
        return isEmpty(collection) ? null : collection.iterator().next();
    }
    /** 获取集合的最后一个元素 */
    public static <T> T last(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }

        // 当类型为 List 时, 直接取得最后一个元素
        if (collection instanceof List) {
            List<T> list = (List<T>) collection;
            return list.get(list.size() - 1);
        }
        // 其他类型通过 iterator 滚动到最后一个元素
        Iterator<T> iterator = collection.iterator();
        while (true) {
            T current = iterator.next();
            if (!iterator.hasNext()) {
                return current;
            }
        }
    }

    /** 集合中随机返回一个 */
    public static <T> T rand(Collection<T> source) {
        return isEmpty(source) ? null : (T) source.toArray()[U.RANDOM.nextInt(source.size())];
    }
}
