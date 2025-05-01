package com.taowater.taol.core.util;

import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 切割字符串工具
 *
 * @author zhu56
 */
@SuppressWarnings("unused")
@UtilityClass
public class SplitUtil {

    /**
     * 切割long类型id
     *
     * @param obj 原对象
     * @return 分割结果
     */
    public static List<Long> splitLong(Object obj) {
        return splitLong(obj, ",");
    }

    /**
     * 切割long类型id
     *
     * @param obj       原对象
     * @param delimiter 分隔符
     * @return 分割结果
     */
    public static List<Long> splitLong(Object obj, String delimiter) {
        return split(obj, delimiter, Long::valueOf, true);
    }

    /**
     * 切割id
     *
     * @param obj 原对象
     * @return 分割结果
     */
    public static List<String> split(Object obj) {
        return split(obj, ",");
    }

    /**
     * 切割id
     *
     * @param obj       原对象
     * @param delimiter 分隔符
     * @return 分割结果
     */
    public static List<String> split(Object obj, String delimiter) {
        return split(obj, delimiter, String::valueOf, true);
    }

    /**
     * 切割字符串
     *
     * @param obj       原对象
     * @param delimiter 分隔符
     * @param action    切分后元素处理逻辑
     * @param distinct  是否去重
     * @return 分割结果
     */
    public static <T> List<T> split(Object obj, String delimiter, Function<String, T> action, boolean distinct) {
        String str = Optional.ofNullable(obj).map(String::valueOf).orElse(null);
        if (Objects.isNull(str)) {
            return new ArrayList<>();
        }
        Stream<T> stream = Arrays.stream(str.split(delimiter)).filter(EmptyUtil::isNotEmpty).map(String::trim).map(action);
        if (distinct) {
            stream = stream.distinct();
        }
        return stream.collect(Collectors.toList());
    }
}
