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
        public <T> Collection<T> instance() {
            return new ArrayList<>();
        }
    },

    /** 去重且无序集合 */
    HASH_SET {
        @Override
        public <T> Collection<T> instance() {
            return new HashSet<>();
        }
    },

    /** 去重且有序集合 */
    LINKED_HASH_SET {
        @Override
        public <T> Collection<T> instance() {
            return new LinkedHashSet<>();
        }
    };

    abstract <T> Collection<T> instance();
}
