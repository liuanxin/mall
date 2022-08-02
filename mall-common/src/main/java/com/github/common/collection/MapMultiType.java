package com.github.common.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

/** 使用 {@link MultiUtil} 的静态方法 */
enum MapMultiType {

    ARRAY_LIST {
        @Override
        public <V> Collection<V> instance() {
            return new ArrayList<>();
        }
    },

    HASH_SET {
        @Override
        public <V> Collection<V> instance() {
            return new HashSet<>();
        }
    },

    LINKED_HASH_SET {
        @Override
        public <V> Collection<V> instance() {
            return new LinkedHashSet<>();
        }
    };

    abstract <V> Collection<V> instance();
}
