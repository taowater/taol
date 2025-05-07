package com.taowater.taol.core.cache;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

/**
 * 缓存
 *
 * @author zhu56
 */
public class Cache<K extends Comparable<K>, V> {

    /**
     * 数据映射
     */
    private final Map<K, V> data;

    private final SortList<K> keys;
    /**
     * 容量
     */
    private int capacity;

    /**
     * 元素至少持有毫秒数，未经过此时间的元素不会被清除
     */
    private static final long AT_LEAST_CACHE_MILLIS = 1000;

    /**
     * 出发清除逻辑时，清除的比例
     */
    private static final float FACTOR = 0.25f;

    /**
     * 最大容量
     */
    private static final int MAXIMUM_CAPACITY = 65535;

    // 添加专用锁对象
    private final Object maintenanceLock = new Object();

    public Cache(int initCapacity) {
        if (initCapacity < 0)
            throw new IllegalArgumentException();
        int cap = ((initCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                MAXIMUM_CAPACITY :
                tableSizeFor(initCapacity + (initCapacity >>> 1) + 1));
        this.capacity = cap;
        this.data = new ConcurrentHashMap<>(cap);
        this.keys = new SortList<>();
    }

    private static int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V value = data.computeIfAbsent(key, mappingFunction);
        keys.add(key);
        performMaintenance();
        return value;
    }

    private void performMaintenance() {
        // 如果满了
        if (size() >= capacity) {
            synchronized (maintenanceLock) {
                long removeNum = (long) (capacity * FACTOR);
                Key<K> oldest = keys.getSet().first();
                // 如果最旧的元素仍未过时，则直接扩容
                // 但如果到达最大允许数量，还是得清除
                if (oldest != null && oldest.getTimestamp() > System.currentTimeMillis() - AT_LEAST_CACHE_MILLIS && capacity < MAXIMUM_CAPACITY / 2) {
                    capacity *= 2;
                } else {
                    // 取新鲜度最差的移除
                    while (removeNum-- > 0) {
                        Key<K> oldKey = keys.pollFirst();
                        if (Objects.nonNull(oldKey)) {
                            data.remove(oldKey.getValue());
                        }
                    }
                }
            }
        }
    }

    private int size() {
        return data.size();
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    public static class Key<T> {
        private T value;
        private long timestamp;

        public Key(T value) {
            this.value = value;
            this.refresh();
        }

        public void refresh() {
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class SortList<K extends Comparable<K>> {
        // 存储 name 到 Data 的映射（保证唯一性）
        private final ConcurrentHashMap<K, Key<K>> nameMap = new ConcurrentHashMap<>();

        private final ConcurrentSkipListSet<Key<K>> sortedSet =
                new ConcurrentSkipListSet<>(Comparator.comparingLong((Key<K> k) -> k.getTimestamp())
                        .thenComparing(Key::getValue));

        public ConcurrentSkipListSet<Key<K>> getSet() {
            return sortedSet;
        }

        public Key<K> pollFirst() {
            Key<K> first = sortedSet.pollFirst();
            if (Objects.nonNull(first)) {
                nameMap.remove(first.getValue());
            }
            return first;

        }

        /**
         * 添加或更新元素（线程安全）
         */
        public void add(K key) {
            nameMap.compute(key, (name, existingData) -> {
                // 如果已存在同名元素，先从 sortedSet 中移除旧值
                if (existingData != null) {
                    sortedSet.remove(existingData);
                    existingData.refresh(); // 复用对象并更新时间戳
                    sortedSet.add(existingData);
                    return existingData;
                } else {
                    Key<K> newKey = new Key<>(key);
                    sortedSet.add(newKey);
                    return newKey;
                }
            });
        }
    }
}
