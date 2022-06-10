package com.github.common.util;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

/** 集合相关的工具包 */
@SuppressWarnings({"rawtypes", "unchecked", "DuplicatedCode"})
public final class A {

    private static final String SPLIT = ",";

    /** 对象是数组或集合则返回 true */
    public static boolean isArray(Object obj) {
        return obj != null && (obj.getClass().isArray() || obj instanceof Collection);
    }
    /** 对象是数组或集合则返回 false */
    public static boolean isNotArray(Object obj) {
        return !isArray(obj);
    }

    /** 对象是空或是数组且长度为 0 或是集合且长度是 0 或是 map 且长度是 0, 则返回 true */
    public static boolean isEmptyObj(Object arrayOrCollectionOrMap) {
        if (arrayOrCollectionOrMap == null) {
            return true;
        }
        if (arrayOrCollectionOrMap.getClass().isArray()) {
            return Array.getLength(arrayOrCollectionOrMap) == 0;
        }
        if (arrayOrCollectionOrMap instanceof Collection c) {
            return c.isEmpty();
        }
        if (arrayOrCollectionOrMap instanceof Map m) {
            return m.isEmpty();
        }
        return false;
    }
    /** 对象是空或是数组且长度为 0 或是集合且长度是 0 或是 map 且长度是 0, 则返回 false */
    public static boolean isNotEmptyObj(Object arrayOrCollectionOrMap) {
        return !isEmptyObj(arrayOrCollectionOrMap);
    }

    /** 数组为空或其每一项都是空则返回 true */
    public static <T> boolean isEmpty(T[] array) {
        if (array == null || array.length == 0) {
            return true;
        }
        for (T t : array) {
            if (t != null) {
                return false;
            }
        }
        return true;
    }
    /** 数组为空或其每一项都是空则返回 false */
    public static <T> boolean isNotEmpty(T[] array) {
        return !isEmpty(array);
    }

    /** 集合为空或其每一项都是空则返回 true */
    public static boolean isEmpty(Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return true;
        }
        for (Object t : collection) {
            if (t != null) {
                return false;
            }
        }
        return true;
    }
    /** 集合为空或其每一项都是空则返回 false */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /** map 为空或其长度为 0 则返回 true */
    public static boolean isEmpty(Map map) {
        return map == null || map.isEmpty();
    }
    /** map 为空或其长度为 0 则返回 false */
    public static boolean isNotEmpty(Map map) {
        return !isEmpty(map);
    }

    /** 拿英文逗号(,)分隔集合(忽略空) */
    public static String toStr(Collection<?> collection) {
        return toStr(collection, SPLIT);
    }
    /** 拿指定字符分隔集合(忽略空) */
    public static String toStr(Collection<?> collection, String split) {
        return toStr(collection, split, true, false);
    }

    /**
     * 拿指定字符分隔集合
     *
     * @param ignoreNull true 表示忽略 null
     * @param ignoreBlank true 表示忽略 空字符
     */
    public static String toStr(Collection<?> collection, String split, boolean ignoreNull, boolean ignoreBlank) {
        if (isEmpty(collection)) {
            return U.EMPTY;
        }

        StringJoiner joiner = new StringJoiner(split);
        for (Object obj : collection) {
            if (U.isNull(obj)) {
                if (!ignoreNull) {
                    joiner.add(null);
                }
            } else {
                String str = obj.toString();
                if (U.isNotBlank(str) || !ignoreBlank) {
                    joiner.add(str);
                }
            }
        }
        return joiner.toString();
    }
    /** 拿英文逗号(,)分隔数组或集合(忽略空) */
    public static String toString(Object arrayOrCollection) {
        return toString(arrayOrCollection, SPLIT);
    }
    /** 拿指定字符分隔数组或集合(忽略空) */
    public static String toString(Object arrayOrCollection, String split) {
        if (U.isNull(arrayOrCollection)) {
            return U.EMPTY;
        }

        if (arrayOrCollection.getClass().isArray()) {
            StringJoiner joiner = new StringJoiner(split);
            int len = Array.getLength(arrayOrCollection);
            for (int i = 0; i < len; i++) {
                Object obj = Array.get(arrayOrCollection, i);
                if (U.isNotNull(obj)) {
                    joiner.add(obj.toString());
                }
            }
            return joiner.toString();
        }
        if (arrayOrCollection instanceof Collection c) {
            return toStr(c, split);
        }
        return arrayOrCollection.toString();
    }

    /** 拿英文逗号(,)分隔数组(忽略空) */
    public static String toStr(Object[] array) {
        return toStr(array, SPLIT);
    }
    /** 拿指定字符分隔数组(忽略空) */
    public static String toStr(Object[] array, String split) {
        return toStr(array, split, true, false);
    }

    /**
     * 拿指定字符分隔数组
     *
     * @param ignoreNull true 表示忽略 null
     * @param ignoreBlank true 表示忽略 空字符
     */
    public static String toStr(Object[] array, String split, boolean ignoreNull, boolean ignoreBlank) {
        if (isEmpty(array)) {
            return U.EMPTY;
        }

        StringJoiner joiner = new StringJoiner(split);
        int len = array.length;
        for (Object obj : array) {
            if (U.isNull(obj)) {
                if (!ignoreNull) {
                    joiner.add(null);
                }
            } else {
                String str = obj.toString();
                if (U.isNotBlank(str) || !ignoreBlank) {
                    joiner.add(str);
                }
            }
        }
        return joiner.toString();
    }

    /** 数组转成集合 */
    public static <T> List<T> arrayToList(Object array) {
        if (U.isNull(array)) {
            return Collections.emptyList();
        }
        if (array instanceof List l) {
            return (List<T>) l;
        }
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("不是数据: " + array);
        }
        int length = Array.getLength(array);
        if (length == 0) {
            return Collections.emptyList();
        }

        List<T> returnList = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            returnList.add((T) Array.get(array, i));
        }
        return returnList;
    }

    /** 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空), 如果同样的 key 有多个值, 后面将覆盖前面 */
    public static <K, T> Map<K, T> listToMap(Collection<T> list, Function<? super T, K> func) {
        return listToMap(new HashMap<>(), list, func, 0);
    }
    /**
     * 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空)
     *
     * @param handle 1.抛异常, 2.已经有了就不再写入, 否则覆盖
     */
    public static <K, T> Map<K, T> listToMap(Collection<T> list, Function<? super T, K> func, int handle) {
        return listToMap(new HashMap<>(), list, func, handle);
    }
    private static <K, T> Map<K, T> listToMap(Map<K, T> returnMap, Collection<T> list, Function<? super T, K> func, int handle) {
        if (isNotEmpty(list)) {
            for (T obj : list) {
                if (U.isNotNull(obj)) {
                    K k = func.apply(obj);
                    if (U.isNotNull(k)) {
                        if (returnMap.containsKey(k)) {
                            switch (handle) {
                                case 1 -> throw new RuntimeException("重复数据不允许写入");
                                case 2 -> {}
                                default -> returnMap.put(k, obj);
                            }
                        } else {
                            returnMap.put(k, obj);
                        }
                    }
                }
            }
        }
        return returnMap;
    }
    /** 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 如果同样的 key 有多个值, 后面将覆盖前面 */
    public static <K, T> Map<K, T> listToLinkedMap(Collection<T> list, Function<? super T, K> func) {
        return listToMap(new LinkedHashMap<>(), list, func, 0);
    }
    /**
     * 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空)
     *
     * @param handle 1.抛异常, 2.已经有了就不再写入, 否则覆盖
     */
    public static <K, T> Map<K, T> listToLinkedMap(Collection<T> list, Function<? super T, K> func, int handle) {
        return listToMap(new LinkedHashMap<>(), list, func, handle);
    }

    /** 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是一个 List */
    public static <K, T> Map<K, List<T>> listToMapList(Collection<T> list, Function<? super T, K> func) {
        Map<K, List<T>> returnMap = new HashMap<>();
        if (isNotEmpty(list)) {
            for (T obj : list) {
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
    /** 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是一个 List */
    public static <K, T> Map<K, List<T>> listToLinkedMapList(Collection<T> list, Function<? super T, K> func) {
        Map<K, List<T>> returnMap = new LinkedHashMap<>();
        if (isNotEmpty(list)) {
            for (T obj : list) {
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

    /** 用指定的方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是一个 LinkedHashSet */
    public static <K, T> Map<K, Set<T>> listToMapSet(Collection<T> list, Function<? super T, K> func) {
        Map<K, Set<T>> returnMap = new HashMap<>();
        if (isNotEmpty(list)) {
            for (T obj : list) {
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
    /** 用指定的方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是一个 LinkedHashSet */
    public static <K, T> Map<K, Set<T>> listToLinkedMapSet(Collection<T> list, Function<? super T, K> func) {
        Map<K, Set<T>> returnMap = new LinkedHashMap<>();
        if (isNotEmpty(list)) {
            for (T obj : list) {
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

    /** 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空), 如果同样的 key 有多个值, 后面将覆盖前面 */
    public static <T, K, V> Map<K, V> listToMapKeyValue(Collection<T> list, Function<? super T, K> keyFun,
                                                        Function<? super T, V> valueFun) {
        return listToMapKeyValue(new HashMap<>(), list, keyFun, valueFun, 0);
    }
    /**
     * 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空)
     *
     * @param handle 1.抛异常, 2.已经有了就不再写入, 否则覆盖
     */
    public static <T, K, V> Map<K, V> listToMapKeyValue(Collection<T> list, Function<? super T, K> keyFun,
                                                        Function<? super T, V> valueFun, int handle) {
        return listToMapKeyValue(new HashMap<>(), list, keyFun, valueFun, handle);
    }
    private static <T, K, V> Map<K, V> listToMapKeyValue(Map<K, V> returnMap, Collection<T> list,
                                                         Function<? super T, K> keyFun,
                                                         Function<? super T, V> valueFun,
                                                         int handle) {
        if (isNotEmpty(list)) {
            for (T obj : list) {
                if (U.isNotNull(obj)) {
                    K k = keyFun.apply(obj);
                    if (U.isNotNull(k)) {
                        V v = valueFun.apply(obj);
                        if (U.isNotNull(v)) {
                            if (returnMap.containsKey(k)) {
                                switch (handle) {
                                    case 1 -> throw new RuntimeException("重复数据不允许写入");
                                    case 2 -> {}
                                    default -> returnMap.put(k, v);
                                }
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
        return listToMapKeyValue(new LinkedHashMap<>(), list, keyFun, valueFun, 0);
    }
    /**
     * 用两个指定方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空)
     *
     * @param handle 1.抛异常, 2.已经有了就不再写入, 否则覆盖
     */
    public static <T, K, V> Map<K, V> listToLinkedMapKeyValue(Collection<T> list, Function<? super T, K> keyFun,
                                                              Function<? super T, V> valueFun, int handle) {
        return listToMapKeyValue(new LinkedHashMap<>(), list, keyFun, valueFun, handle);
    }

    /** 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是一个 List */
    public static <T, K, V> Map<K, List<V>> listToMapKeyValueList(Collection<T> list,
                                                                  Function<? super T, K> keyFun,
                                                                  Function<? super T, V> valueFun) {
        Map<K, List<V>> returnMap = new HashMap<>();
        if (isNotEmpty(list)) {
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
    /** 用两个指定方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是一个 List */
    public static <T, K, V> Map<K, List<V>> listToLinkedMapKeyValueList(Collection<T> list,
                                                                        Function<? super T, K> keyFun,
                                                                        Function<? super T, V> valueFun) {
        Map<K, List<V>> returnMap = new LinkedHashMap<>();
        if (isNotEmpty(list)) {
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

    /** 用两个指定方法将 List 转换成 HashMap(过滤 key 和 value 为空), 其中 map 的 value 是一个 LinkedHashSet */
    public static <T, K, V> Map<K, Set<V>> listToMapKeyValueSet(Collection<T> list,
                                                                Function<? super T, K> keyFun,
                                                                Function<? super T, V> valueFun) {
        Map<K, Set<V>> returnMap = new HashMap<>();
        if (isNotEmpty(list)) {
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
    /** 用两个指定方法将 List 转换成 LinkedHashMap(过滤 key 和 value 为空), 其中 map 的 value 是一个 LinkedHashSet */
    public static <T, K, V> Map<K, Set<V>> listToLinkedMapKeyValueSet(Collection<T> list,
                                                                      Function<? super T, K> keyFun,
                                                                      Function<? super T, V> valueFun) {
        Map<K, Set<V>> returnMap = new LinkedHashMap<>();
        if (isNotEmpty(list)) {
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

    /** 将 List 中指定的方法收集了并返回(过滤空) */
    public static <T, R> List<R> collect(Collection<T> list, Function<T, R> func) {
        List<R> returnList = new ArrayList<>();
        if (isNotEmpty(list)) {
            for (T obj : list) {
                if (U.isNotNull(obj)) {
                    R value = func.apply(obj);
                    if (U.isNotNull(value)) {
                        returnList.add(value);
                    }
                }
            }
        }
        return returnList;
    }

    /** 将 List 中指定的方法收集了去重并返回(过滤空) */
    public static <T, R> List<R> collectDistinct(Collection<T> list, Function<T, R> func) {
        Set<R> returnSet = new LinkedHashSet<>();
        if (isNotEmpty(list)) {
            for (T obj : list) {
                if (U.isNotNull(obj)) {
                    R value = func.apply(obj);
                    if (U.isNotNull(value)) {
                        returnSet.add(value);
                    }
                }
            }
        }
        return new ArrayList<>(returnSet);
    }

    /** 将 List 分割成多个 List */
    public static <T> List<List<T>> split(Collection<T> list, int singleSize) {
        List<List<T>> returnList = new ArrayList<>();
        if (isNotEmpty(list) && singleSize > 0) {
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
                    innerList.add(getIndex(list, j));
                }
                returnList.add(innerList);
            }
        }
        return returnList;
    }

    /** 数组去重返回 */
    public static <T> List<T> duplicate(T[] array) {
        return duplicate(Arrays.asList(array));
    }
    /** 删除重复的项 */
    public static <T> List<T> duplicate(Collection<T> list) {
        return new ArrayList(new LinkedHashSet<>(list));
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

    /** 构造 ArrayList, 过滤 null 值 */
    public static <T> List<T> newArrayListSkipNull(T... objs) {
        return list(new ArrayList<>(), true, objs);
    }
    /** 构造 ArrayList */
    public static <T> List<T> newArrayList(T... objs) {
        return list(new ArrayList<>(), false, objs);
    }
    private static <T> List<T> list(List<T> result, boolean skipNull, T... objs) {
        if (isNotEmpty(objs)) {
            for (T obj : objs) {
                if (U.isNotNull(obj) || !skipNull) {
                    result.add(obj);
                }
            }
        }
        return result;
    }
    /** 构造 LinkedList, 过滤 null 值 */
    public static <T> List<T> newLinkedListSkipNull(T... objs) {
        return list(new LinkedList<>(), true, objs);
    }
    /** 构造 LinkedList */
    public static <T> List<T> newLinkedList(T... objs) {
        return list(new LinkedList<>(), false, objs);
    }

    /** 获取数组的第一个元素 */
    public static <T> T first(T[] array) {
        return isEmpty(array) ? null : array[0];
    }
    /** 获取数组的最后一个元素 */
    public static <T> T last(T[] array) {
        return isEmpty(array) ? null : array[array.length - 1];
    }
    /** 获取数组指定索引的值 */
    public static <T> T getIndex(T[] array, int index) {
        return (isEmpty(array) || index < 0 || index >= array.length) ? null : array[index];
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
        if (collection instanceof List<T> list) {
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
    /** 获取集合指定下标的值 */
    public static <T> T getIndex(Collection<T> collection, int index) {
        if (isEmpty(collection) || index < 0 || index >= collection.size()) {
            return null;
        }
        // 当类型为 List 时, 直接取最后一个元素
        if (collection instanceof List<T> list) {
            return list.get(index);
        }

        int i = 0;
        Iterator<T> iterator = collection.iterator();
        while (true) {
            T current = iterator.next();
            if (index == i) {
                return current;
            }
            i++;
        }
    }

    /** 集合中随机返回一个 */
    public static <T> T rand(Collection<T> source) {
        return isEmpty(source) ? null : (T) source.toArray()[U.RANDOM.nextInt(source.size())];
    }
}
