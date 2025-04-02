package com.taowater.taol.core.util;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * 集合相关工具
 */
@UtilityClass
public class CollUtil {
    
    public static <T> T get(final Collection<T> collection, int index) {
        if (EmptyUtil.isEmpty(collection)) {
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
        if (EmptyUtil.isEmpty(array)) {
            return null;
        }
        if (index < 0 || index >= array.length) {
            return null;
        }
        return array[index];
    }

    public static <E> E get(Iterator<E> iterator, int index) {
        if (EmptyUtil.isEmpty(iterator)) {
            return null;
        }
        if (index < 0) {
            return null;
        }
        while (iterator.hasNext()) {
            index--;
            if (-1 == index) {
                return iterator.next();
            }
            iterator.next();
        }
        return null;
    }
}
