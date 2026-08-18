package com.taowater.taol.core.util;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Array;
import java.util.*;

/**
 * 集合相关工具
 */
@UtilityClass
public class CollUtil {

    public static <T> T get(final Collection<T> collection, int index) {
        if (Objects.isNull(collection)) {
            return null;
        }
        // 检查越界
        if (index < 0 || index >= collection.size()) {
            return null;
        }

        if (collection instanceof List) {
            return ((List<T>) collection).get(index);
        }
        return get(collection.iterator(), index);
    }

    public static <E> E get(E[] array, int index) {
        if (Objects.isNull(array)) {
            return null;
        }
        if (index < 0 || index >= array.length) {
            return null;
        }
        return array[index];
    }

    public static <E> E get(Iterator<E> iterator, final int index) {
        if (Objects.isNull(iterator)) {
            return null;
        }
        if (index < 0) {
            return null;
        }
        int tempIndex = index;
        while (iterator.hasNext()) {
            tempIndex--;
            if (-1 == tempIndex) {
                return iterator.next();
            }
            iterator.next();
        }
        return null;
    }


    @SafeVarargs
    public static <E> List<E> list(List<E> defaultList, E... elements) {
        if (Objects.isNull(elements)) {
            return defaultList;
        }
        List<E> list = new ArrayList<>(elements.length);
        list.addAll(Arrays.asList(elements));
        return list;
    }

    @SafeVarargs
    public static <E> List<E> list(E... elements) {
        return list(Collections.emptyList(), elements);
    }

    @SafeVarargs
    public static <E> Set<E> set(Set<E> defaultSet, E... elements) {
        if (Objects.isNull(elements)) {
            return defaultSet;
        }
        Set<E> set = new HashSet<>(elements.length);
        set.addAll(Arrays.asList(elements));
        return set;
    }

    @SafeVarargs
    public static <E> Set<E> set(E... elements) {
        return set(Collections.emptySet(), elements);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] arr(T obj, int size) {
        if (Objects.isNull(obj)) {
            return (T[]) new Object[size];
        }
        T[] arr = (T[]) Array.newInstance(obj.getClass(), size);
        Arrays.fill(arr, obj);
        return arr;
    }
}
