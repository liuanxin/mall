package com.github.common.collection;

import java.util.*;

public final class MapValueSet<K, V> {

    private final Map<K, Set<V>> valueMap;
    private final boolean valueHasSort;
    MapValueSet(boolean hasSort, boolean valueHasSort) {
        this.valueMap = hasSort ? new LinkedHashMap<>() : new HashMap<>();
        this.valueHasSort = valueHasSort;
    }

    private Set<V> computeIfAbsent(K k) {
        return valueMap.computeIfAbsent(k, (k1) -> valueHasSort ? new LinkedHashSet<>() : new HashSet<>());
    }

    public void put(K k, V v) {
        computeIfAbsent(k).add(v);
    }

    public void remove(K k, V v) {
        computeIfAbsent(k).remove(v);
    }

    public Set<V> get(K k) {
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

    public Map<K, Set<V>> asMap() {
        return valueMap;
    }

    @Override
    public String toString() {
        return valueMap.toString();
    }
}
