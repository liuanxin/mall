package com.github.common.collection;

import java.util.Collection;
import java.util.Map;

/** 使用 {@link MultiUtil} 的静态方法 */
public final class MapMultiValue<K, V, C extends Collection<V>> {

    private final Map<K, C> valueMap;
    private final MapMultiType valueType;
    MapMultiValue(Map<K, C> valueMap, MapMultiType valueType) {
        this.valueMap = valueMap;
        this.valueType = valueType;
    }

    @SuppressWarnings("unchecked")
    private C computeIfAbsent(K k) {
        return valueMap.computeIfAbsent(k, (k1) -> (C) valueType.instance());
    }

    public void put(K k, V v) {
        computeIfAbsent(k).add(v);
    }

    public void remove(K k, V v) {
        computeIfAbsent(k).remove(v);
    }

    public C get(K k) {
        return valueMap.get(k);
    }

    public void remove(K k) {
        valueMap.remove(k);
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
