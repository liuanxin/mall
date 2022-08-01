package com.github.common.collection;

import java.util.*;

public final class MapValueList<K, V> {

    private final Map<K, List<V>> valueMap;
    MapValueList(boolean hasSort) {
        this.valueMap = hasSort ? new LinkedHashMap<>() : new HashMap<>();
    }

    private List<V> computeIfAbsent(K k) {
        return valueMap.computeIfAbsent(k, (k1) -> new ArrayList<>());
    }

    public void put(K k, V v) {
        computeIfAbsent(k).add(v);
    }

    public void remove(K k, V v) {
        computeIfAbsent(k).remove(v);
    }

    public List<V> get(K k) {
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

    public Map<K, List<V>> asMap() {
        return valueMap;
    }

    @Override
    public String toString() {
        return valueMap.toString();
    }
}
