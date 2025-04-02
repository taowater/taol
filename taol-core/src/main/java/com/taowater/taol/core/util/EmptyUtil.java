package com.taowater.taol.core.util;


import com.taowater.taol.core.bo.Tuple;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 判空工具类
 *
 * @author Zhu56
 * @date 2021/10/16 2:05
 */
@UtilityClass
public class EmptyUtil {

    private static final List<Tuple<Predicate<Object>, Predicate<Object>>> STRATEGY = new ArrayList<>();

    static {
        Collections.addAll(STRATEGY,
                Tuple.of(Objects::isNull, o -> true),
                Tuple.of(CharSequence.class::isInstance, o -> ((CharSequence) o).length() == 0),
                Tuple.of(Iterator.class::isInstance, o -> !((Iterator<?>) o).hasNext()),
                Tuple.of(Iterable.class::isInstance, o -> EmptyUtil.isEmpty(((Iterable<?>) o).iterator())),
                Tuple.of(Map.class::isInstance, o -> ((Map<?, ?>) o).isEmpty()),
                Tuple.of(o -> o.getClass().isArray(), o -> Array.getLength(o) == 0),
                Tuple.of(o -> true, o -> false)
        );
    }

    /**
     * 判断任意对象是否为空
     *
     * @param obj 任意对象
     * @return 判断结果
     */
    public static boolean isEmpty(Object obj) {
        return STRATEGY
                .stream()
                .filter(e -> e.getLeft().test(obj))
                .findFirst()
                .map(Tuple::getRight)
                .map(e -> e.test(obj))
                .orElse(false);
    }

    /**
     * 判断任意对象是否不为空
     *
     * @param obj 任意对象
     * @return 判断结果
     */
    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    /**
     * 判断对象是否至少一个为空 主要包括字符串，集合，数组，及其他类型
     *
     * @param objs 若干个对象
     * @return 判断结果
     */
    public static boolean isHadEmpty(Object... objs) {
        return Stream.of(objs).anyMatch(EmptyUtil::isEmpty);
    }

    /**
     * 判断若干个对象中是否存在非空
     *
     * @param objs 若干个对象
     * @return 判断结果
     */
    public static boolean isHadNotEmpty(Object... objs) {
        return Stream.of(objs).anyMatch(EmptyUtil::isNotEmpty);
    }

    /**
     * 判断若干个对象是否全空
     *
     * @param objs 若干个对象
     * @return 判断结果
     */
    public static boolean isAllEmpty(Object... objs) {
        return !isHadNotEmpty(objs);
    }

    /**
     * 判断若干个对象中是否全为非空
     *
     * @param objs 若干个对象
     * @return 判断结果
     */
    public static boolean isAllNotEmpty(Object... objs) {
        return !isHadEmpty(objs);
    }

    /**
     * 判断若干个对象中是否既有空的，也有非空的
     *
     * @param objs 若干个对象
     * @return 判断结果
     */
    public static boolean isHadBoth(Object... objs) {
        return isHadEmpty(objs) && isHadNotEmpty(objs);
    }

    /**
     * 是否为空白串
     *
     * @param str 字符串
     * @return boolean
     */
    public static boolean isBlank(CharSequence str) {
        return isEmpty(str) || str.toString().trim().isEmpty();
    }

    /**
     * 是否为非空白串
     *
     * @param str 字符串
     * @return boolean
     */
    public static boolean isNotBlank(CharSequence str) {
        return !isBlank(str);
    }
}
