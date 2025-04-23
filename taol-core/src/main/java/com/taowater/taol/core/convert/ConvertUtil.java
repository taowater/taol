package com.taowater.taol.core.convert;


import com.taowater.taol.core.bo.Tuple;
import com.taowater.taol.core.function.LambdaUtil;
import com.taowater.taol.core.reflect.ClassUtil;
import com.taowater.taol.core.reflect.ReflectUtil;
import com.taowater.taol.core.util.EmptyUtil;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 转换工具类
 *
 * @author zhu56
 * @date 2023/05/03 23:29
 */
@UtilityClass
public class ConvertUtil {

    private static final Map<String, List<Tuple<Function<?, ?>, BiConsumer<?, ?>>>> GS_CACHE = new ConcurrentHashMap<>();

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
     * @param source 源
     * @param target 目标
     */
    @SuppressWarnings("unchecked")
    public static <S, T> void copy(S source, T target) {
        if (EmptyUtil.isHadEmpty(source, target)) {
            return;
        }
        List<Tuple<Function<?, ?>, BiConsumer<?, ?>>> gs = gs((Class<S>) source.getClass(), (Class<T>) target.getClass());

        if (EmptyUtil.isEmpty(gs)) {
            return;
        }
        assert gs != null;
        gs.forEach(t -> {
            Function<S, Object> getter = (Function<S, Object>) t.getLeft();
            BiConsumer<T, Object> setter = (BiConsumer<T, Object>) t.getRight();
            Object o = getter.apply(source);
            if (Objects.nonNull(o)) {
                setter.accept(target, o);
            }
        });
    }

    // todo 缓存
    private static <S, T> List<Tuple<Function<?, ?>, BiConsumer<?, ?>>> gs(Class<S> sourceClazz, Class<T> targetClazz) {
        if (EmptyUtil.isHadEmpty(sourceClazz, targetClazz)) {
            return null;
        }

        return GS_CACHE.computeIfAbsent(sourceClazz.getName() + "_" + targetClazz.getName(), k -> {
            List<Field> targetFields = ReflectUtil.getFields(targetClazz);
            if (EmptyUtil.isEmpty(targetFields)) {
                return null;
            }
            return targetFields.stream().map(field -> {
                String fieldName = field.getName();
                Function<S, Object> getter = LambdaUtil.buildGetter(sourceClazz, field);
                if (Objects.isNull(getter)) {
                    return null;
                }
                BiConsumer<T, Object> setter = LambdaUtil.buildSetter(targetClazz, fieldName);
                return Tuple.<Function<?, ?>, BiConsumer<?, ?>>of(getter, setter);
            }).filter(Objects::nonNull).collect(Collectors.toList());
        });
    }
}
