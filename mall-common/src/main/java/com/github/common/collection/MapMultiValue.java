package com.github.common.collection;

import java.util.Collection;
import java.util.Map;

/** 使用 {@link MapMultiUtil} 的静态方法 */
public final class MapMultiValue<K, V, C extends Collection<V>> {

    private final Map<K, C> valueMap;
    private final MapValueMultiType valueType;
    MapMultiValue(Map<K, C> valueMap, MapValueMultiType valueType) {
        this.valueMap = valueMap;
        this.valueType = valueType;
    }

    public boolean put(K k, V v) {
        // noinspection unchecked
        return valueMap.computeIfAbsent(k, (k1) -> (C) valueType.instance()).add(v);
    }

    public boolean remove(K k, V v) {
        return valueMap.containsKey(k) && valueMap.get(k).remove(v);
    }

    public C remove(K k) {
        return valueMap.remove(k);
    }

    public C get(K k) {
        return valueMap.get(k);
    }

    public boolean isEmpty() {
        return valueMap.isEmpty();
    }
    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public Map<K, C> asMap() {
        return valueMap;
    }

    @Override
    public String toString() {
        return valueMap.toString();
    }
}
