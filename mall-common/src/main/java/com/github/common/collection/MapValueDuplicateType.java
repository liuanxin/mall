package com.github.common.collection;

import java.util.Collection;
import java.util.Map;

/** list 转换成 map 时, key 重复后的做法 */
enum MapValueDuplicateType {

    /** 抛异常 */
    THROW {
        @Override
        public <K, V> void handle(Map<K, V> map, Collection<?> list, K k, V v) {
            throw new RuntimeException(String.format("Duplicate (%s : %s) in data (%s)", k, v, list));
        }
    },

    /** 覆盖 */
    COVER {
        @Override
        public <K, V> void handle(Map<K, V> map, Collection<?> list, K k, V v) {
            map.put(k, v);
        }
    },

    /** 忽略 */
    IGNORE {
        @Override
        public <K, V> void handle(Map<K, V> map, Collection<?> list, K k, V v) {
        }
    };

    abstract <K, V> void handle(Map<K, V> map, Collection<?> list, K k, V v);
}
