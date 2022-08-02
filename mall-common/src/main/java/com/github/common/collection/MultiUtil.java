package com.github.common.collection;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class MultiUtil {

    /** 生成 HashMap&lt;K, ArrayList&lt;V&gt;&gt; 实体(value 是数组), 只需要做 add remove, 用 asMap 成最终实体 */
    public static <K, V> MapMultiValue<K, V, List<V>> createMapList() {
        return new MapMultiValue<>(new HashMap<>(), MapMultiType.ARRAY_LIST);
    }
    /** 生成 LinkedHashMap&lt;K, ArrayList&lt;V&gt;&gt; 实体(value 是数组), 只需要做 add remove, 用 asMap 成最终实体 */
    public static <K, V> MapMultiValue<K, V, List<V>> createLinkedMapList() {
        return new MapMultiValue<>(new LinkedHashMap<>(), MapMultiType.ARRAY_LIST);
    }

    /** 生成 HashMap&lt;K, HashSet&lt;V&gt;&gt; 实体(value 是去重且无序的数组), 只需要做 add remove, 用 asMap 成最终实体 */
    public static <K, V> MapMultiValue<K, V, Set<V>> createMapSet() {
        return new MapMultiValue<>(new HashMap<>(), MapMultiType.HASH_SET);
    }
    /** 生成 LinkedHashMap&lt;K, HashSet&lt;V&gt;&gt; 实体(value 是去重且无序的数组), 只需要做 add remove, 用 asMap 成最终实体 */
    public static <K, V> MapMultiValue<K, V, Set<V>> createLinkedMapSet() {
        return new MapMultiValue<>(new LinkedHashMap<>(), MapMultiType.HASH_SET);
    }

    /** 生成 HashMap&lt;K, LinkedHashSet&lt;V&gt;&gt; 实体(value 是去重且有序的数组), 只需要做 add remove, 用 asMap 成最终实体 */
    public static <K, V> MapMultiValue<K, V, Set<V>> createMapLinkedSet() {
        return new MapMultiValue<>(new HashMap<>(), MapMultiType.LINKED_HASH_SET);
    }
    /** 生成 LinkedHashMap&lt;K, LinkedHashSet&lt;V&gt;&gt; 实体(value 是去重且有序的数组), 只需要做 add remove, 用 asMap 成最终实体 */
    public static <K, V> MapMultiValue<K, V, Set<V>> createLinkedMapLinkedSet() {
        return new MapMultiValue<>(new LinkedHashMap<>(), MapMultiType.LINKED_HASH_SET);
    }
}
