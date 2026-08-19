package com.taowater.taol.core.util;


import com.taowater.taol.core.inter.Emptyable;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * 判空工具类
 *
 * @author Zhu56
 */
@UtilityClass
@SuppressWarnings("unused")
public class EmptyUtil {
    /**
     * 判断任意对象是否为空
     */
    public static boolean isEmpty(Object o) {
        if (Objects.isNull(o)) {
            return true;
        }
        if (o instanceof Emptyable) {
            return ((Emptyable) o).isEmpty();
        }
        if (o instanceof CharSequence) {
            return ((CharSequence) o).length() == 0;
        }
        if (o instanceof Iterator) {
            return !((Iterator<?>) o).hasNext();
        }
        if (o instanceof Iterable) {
            return isEmpty(((Iterable<?>) o).iterator());
        }
        if (o instanceof Map) {
            return ((Map<?, ?>) o).isEmpty();
        }
        if (o.getClass().isArray()) {
            return Array.getLength(o) == 0;
        }
        return false;
    }

    /**
     * 判断任意对象是否不为空
     */
    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    /**
     * 判断对象是否至少一个为空 主要包括字符串，集合，数组，及其他类型
     */
    public static boolean isHadEmpty(Object... objs) {
        if (Objects.isNull(objs)) {
            return true;
        }
        for (Object o : objs) {
            if (EmptyUtil.isEmpty(o)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断若干个对象中是否存在null
     */
    public static boolean isHadNull(Object... objs) {
        if (Objects.isNull(objs)) {
            return true;
        }
        for (Object o : objs) {
            if (Objects.isNull(o)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断若干个对象中是否存在非空
     */
    public static boolean isHadNotEmpty(Object... objs) {
        if (Objects.isNull(objs)) {
            return false;
        }
        for (Object o : objs) {
            if (EmptyUtil.isNotEmpty(o)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断若干个对象中是否存在非null
     */
    public static boolean isHadNotNull(Object... objs) {
        if (Objects.isNull(objs)) {
            return false;
        }
        for (Object o : objs) {
            if (Objects.nonNull(o)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断若干个对象是否全空
     */
    public static boolean isAllEmpty(Object... objs) {
        if (Objects.isNull(objs)) {
            return true;
        }
        for (Object o : objs) {
            if (EmptyUtil.isNotEmpty(o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断若干个对象中是否全为非空
     */
    public static boolean isAllNotEmpty(Object... objs) {
        if (Objects.isNull(objs)) {
            return false;
        }
        for (Object o : objs) {
            if (EmptyUtil.isEmpty(o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断若干个对象中是否既有空的，也有非空的
     */
    public static boolean isHadBoth(Object... objs) {
        if (Objects.isNull(objs)) {
            return false;
        }
        boolean hadNull = false;
        boolean hadNotNull = false;
        for (Object o : objs) {
            if (EmptyUtil.isEmpty(o)) {
                hadNull = true;
            } else {
                hadNotNull = true;
            }
            if (hadNull && hadNotNull) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否为空白串
     */
    public static boolean isBlank(CharSequence str) {
        return Objects.isNull(str) || str.length() == 0 || str.toString().trim().isEmpty();
    }

    /**
     * 是否为非空白串
     */
    public static boolean isNotBlank(CharSequence str) {
        return !isBlank(str);
    }
}
