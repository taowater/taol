package com.taowater.taol.core.convert;


import com.taowater.taol.core.reflect.AssignableUtil;
import com.taowater.taol.core.reflect.ClassUtil;
import com.taowater.taol.core.util.EmptyUtil;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
        if (EmptyUtil.isHadEmpty(source, target)) {
            return;
        }
        BeanMetadata sourceMetadata = BeanMetadata.of(source.getClass());
        BeanMetadata targetMetadata = BeanMetadata.of(target.getClass());
        targetMetadata.getFieldMap().forEach((k, v) -> {
            FieldMetadata sourceField = sourceMetadata.getField(k);
            if (sourceField == null) {
                return;
            }
            Type sourceFieldType = sourceField.getType();
            Type targetFieldType = v.getType();
            if (!AssignableUtil.isAssignable(sourceFieldType, targetFieldType)) {
                return;
            }
            BiConsumer<T, Object> setter = (BiConsumer<T, Object>) targetMetadata.getSetter(k);
            Function<S, Object> getter = (Function<S, Object>) sourceMetadata.getGetter(k);
            if (EmptyUtil.isHadEmpty(getter, setter)) {
                return;
            }
            Object o = getter.apply(source);
            if (Objects.isNull(o)) {
                return;
            }
            setter.accept(target, o);
        });
    }
}
