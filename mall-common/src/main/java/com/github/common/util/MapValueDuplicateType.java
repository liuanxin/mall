package com.github.common.util;

import java.util.Collection;
import java.util.Map;

/** list 转换成 map 时, key 重复后的做法 */
public enum MapValueDuplicateType {

    /** 抛异常 */
    THROW {
        @Override
        public <K, V> void handle(Map<K, V> map, Collection<?> list, K k, V v) {
            throw new RuntimeException(String.format("Duplicate key(%s) in data(%s), value(%s)", k, list, v));
        }
    },

    /** 覆盖(用后写入的为准) */
    COVER {
        @Override
        public <K, V> void handle(Map<K, V> map, Collection<?> list, K k, V v) {
            map.put(k, v);
        }
    },

    /** 忽略(用先写入的为准) */
    IGNORE {
        @Override
        public <K, V> void handle(Map<K, V> map, Collection<?> list, K k, V v) {
        }
    };

    public abstract <K, V> void handle(Map<K, V> map, Collection<?> list, K k, V v);
}
