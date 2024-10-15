package com.taowater.taol.core.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 切割字符串工具
 *
 * @author zhu56
 * @date 2024/10/16 01:44
 */
@UtilityClass
public class SplitUtil {

    /**
     * 切割long类型id
     *
     * @param obj 原对象
     * @return 分割结果
     */
    public static List<Long> splitLongIds(Object obj) {
        return splitLongIds(obj, ",");
    }

    /**
     * 切割long类型id
     *
     * @param obj       原对象
     * @param delimiter 分隔符
     * @return 分割结果
     */
    public static List<Long> splitLongIds(Object obj, String delimiter) {
        return split(obj, delimiter, Long::valueOf, true);
    }

    /**
     * 切割id
     *
     * @param obj 原对象
     * @return 分割结果
     */
    public static List<String> splitIds(Object obj) {
        return splitIds(obj, ",");
    }

    /**
     * 切割id
     *
     * @param obj       原对象
     * @param delimiter 分隔符
     * @return 分割结果
     */
    public static List<String> splitIds(Object obj, String delimiter) {
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
        String str = Optional.ofNullable(obj).filter(e -> EmptyUtil.isNotEmpty(obj)).map(String::valueOf).orElse(null);
        if (EmptyUtil.isEmpty(str)) {
            return new ArrayList<>();
        }
        Stream<T> ztream = Arrays.stream(str.split(delimiter)).filter(EmptyUtil::isNotEmpty).map(String::trim).map(action);
        if (distinct) {
            ztream = ztream.distinct();
        }
        return ztream.collect(Collectors.toList());
    }
}
