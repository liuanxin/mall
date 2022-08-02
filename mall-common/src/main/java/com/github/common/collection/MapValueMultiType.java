package com.github.common.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

/** 键值对的 value 是集合时, value 的类型 */
enum MapValueMultiType {

    /** 数组 */
    ARRAY_LIST {
        @Override
        public <V> Collection<V> instance() {
            return new ArrayList<>();
        }
    },

    /** 去重且无序数组 */
    HASH_SET {
        @Override
        public <V> Collection<V> instance() {
            return new HashSet<>();
        }
    },

    /** 去重且有序数据 */
    LINKED_HASH_SET {
        @Override
        public <V> Collection<V> instance() {
            return new LinkedHashSet<>();
        }
    };

    abstract <V> Collection<V> instance();
}
