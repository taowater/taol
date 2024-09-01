package com.taowater.taol.core.util;

import io.vavr.Tuple;
import io.vavr.Tuple2;
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

    private final List<Tuple2<Predicate<Object>, Predicate<Object>>> STRATEGY = new ArrayList<>();

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
    public boolean isEmpty(Object obj) {
        return STRATEGY
                .stream()
                .filter(e -> e._1.test(obj))
                .findFirst()
                .map(Tuple2::_2)
                .map(e -> e.test(obj))
                .orElse(false);
    }

    /**
     * 判断任意对象是否不为空
     *
     * @param obj 任意对象
     * @return 判断结果
     */
    public boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    /**
     * 判断对象是否至少一个为空 主要包括字符串，集合，数组，及其他类型
     *
     * @param objs 若干个对象
     * @return 判断结果
     */
    public boolean isHadEmpty(Object... objs) {
        return Stream.of(objs).anyMatch(EmptyUtil::isEmpty);
    }

    /**
     * 判断若干个对象中是否存在非空
     *
     * @param objs 若干个对象
     * @return 判断结果
     */
    public boolean isHadNotEmpty(Object... objs) {
        return Stream.of(objs).anyMatch(EmptyUtil::isNotEmpty);
    }

    /**
     * 判断若干个对象是否全空
     *
     * @param objs 若干个对象
     * @return 判断结果
     */
    public boolean isAllEmpty(Object... objs) {
        return !isHadNotEmpty(objs);
    }

    /**
     * 判断若干个对象中是否全为非空
     *
     * @param objs 若干个对象
     * @return 判断结果
     */
    public boolean isAllNotEmpty(Object... objs) {
        return !isHadEmpty(objs);
    }

    /**
     * 判断若干个对象中是否既有空的，也有非空的
     *
     * @param objs 若干个对象
     * @return 判断结果
     */
    public boolean isHadBoth(Object... objs) {
        return isHadEmpty(objs) && isHadNotEmpty(objs);
    }
}
