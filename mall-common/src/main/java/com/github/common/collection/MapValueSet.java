package com.github.common.collection;

import java.util.*;
import java.util.function.Function;

public final class MapValueSet<K, V> {

    private final Map<K, Set<V>> valueMap;
    private final boolean valueHasSort;
    MapValueSet(boolean hasSort, boolean valueHasSort) {
        this.valueMap = hasSort ? new LinkedHashMap<>() : new HashMap<>();
        this.valueHasSort = valueHasSort;
    }

    public void put(K k, V v) {
        valueMap.computeIfAbsent(k, valueFunc(k)).add(v);
    }

    private Function<K, Set<V>> valueFunc(K k) {
        return (key) -> valueHasSort ? new LinkedHashSet<>() : new HashSet<>();
    }

    public void remove(K k, V v) {
        valueMap.computeIfAbsent(k, valueFunc(k)).remove(v);
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
