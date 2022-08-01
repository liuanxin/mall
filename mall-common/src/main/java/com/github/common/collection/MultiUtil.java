package com.github.common.collection;

public class MultiUtil {

    /** 生成一个 HashMap&lt;K, ArrayList&lt;V&gt;&gt; 的实体, 只需要往里做 add remove 操作, 用 asMap 转发成最终实体 */
    public static <K, V> MapValueList<K, V> createMapList() {
        return new MapValueList<>(false);
    }
    /** 生成一个 LinkedHashMap&lt;K, ArrayList&lt;V&gt;&gt; 的实体, 只需要往里做 add remove 操作, 用 asMap 转发成最终实体 */
    public static <K, V> MapValueList<K, V> createLinkedMapList() {
        return new MapValueList<>(true);
    }

    /** 生成一个 HashMap&lt;K, HashSet&lt;V&gt;&gt; 的实体, 只需要往里做 add remove 操作, 用 asMap 转发成最终实体 */
    public static <K, V> MapValueSet<K, V> createMapSet() {
        return new MapValueSet<>(false, false);
    }
    /** 生成一个 LinkedHashMap&lt;K, HashSet&lt;V&gt;&gt; 的实体, 只需要往里做 add remove 操作, 用 asMap 转发成最终实体 */
    public static <K, V> MapValueSet<K, V> createLinkedMapSet() {
        return new MapValueSet<>(true, false);
    }

    /** 生成一个 HashMap&lt;K, LinkedHashSet&lt;V&gt;&gt; 的实体, 只需要往里做 add remove 操作, 用 asMap 转发成最终实体 */
    public static <K, V> MapValueSet<K, V> createMapLinkedSet() {
        return new MapValueSet<>(false, true);
    }
    /** 生成一个 LinkedHashMap&lt;K, LinkedHashSet&lt;V&gt;&gt; 的实体, 只需要往里做 add remove 操作, 用 asMap 转发成最终实体 */
    public static <K, V> MapValueSet<K, V> createLinkedMapLinkedSet() {
        return new MapValueSet<>(true, true);
    }
}
