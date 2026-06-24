package com.taowater.taol.core.convert;


import com.taowater.taol.core.reflect.ClassUtil;
import lombok.experimental.UtilityClass;

import java.util.Objects;

/**
 * Bean 拷贝入口。
 * <p>
 * 调用链：{@link #copy} → {@link BeanCopyPlan}（按类型对缓存）→ 预编译 {@link FieldCopyAction}。
 * 同名属性按 getter/setter 匹配；数值与 char/boolean 走 fast path，其余走 {@link CopyValueConverter}。
 */
@UtilityClass
public class ConvertUtil {

    /**
     * 新建目标实例并拷贝同名属性。
     */
    public static <S, T> T convert(S source, Class<T> tClazz) {
        T target = ClassUtil.newInstance(tClazz);
        copy(source, target);
        return target;
    }

    /**
     * 将 source 同名属性写入已有 target。
     * null 入参直接返回，不抛异常。
     */
    public static <S, T> void copy(S source, T target) {
        if (Objects.isNull(source) || Objects.isNull(target)) {
            return;
        }
        BeanCopyPlan.of(source.getClass(), target.getClass()).copy(source, target);
    }
}
