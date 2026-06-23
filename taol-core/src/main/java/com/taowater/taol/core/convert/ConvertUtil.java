package com.taowater.taol.core.convert;


import com.taowater.taol.core.reflect.ClassUtil;
import lombok.experimental.UtilityClass;

import java.util.Objects;

/**
 * 转换工具类
 *
 * @author zhu56
 * @date 2023/05/03 23:29
 */
@UtilityClass
public class ConvertUtil {

    /**
     * 转换
     *
     * @param source 源
     * @param tClazz t clazz
     * @return {@link T}
     */
    public static <S, T> T convert(S source, Class<T> tClazz) {
        T target = ClassUtil.newInstance(tClazz);
        copy(source, target);
        return target;
    }

    /**
     * 拷贝
     *
     * @param source 源头对象
     * @param target 目标对象
     */
    @SuppressWarnings("unchecked")
    public static <S, T> void copy(S source, T target) {
        if (Objects.isNull(source) || Objects.isNull(target)) {
            return;
        }
        BeanCopyPlan.of(source.getClass(), target.getClass()).copy(source, target);
    }
}
