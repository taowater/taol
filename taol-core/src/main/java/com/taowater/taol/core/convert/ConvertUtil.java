package com.taowater.taol.core.convert;


import com.taowater.taol.core.function.LambdaUtil;
import com.taowater.taol.core.reflect.AssignableUtil;
import com.taowater.taol.core.reflect.ClassUtil;
import com.taowater.taol.core.reflect.ReflectUtil;
import com.taowater.taol.core.util.EmptyUtil;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
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

    /**
     * 类的全限定类名与类的字段信息缓存
     */
    private static final Map<String, Set<FieldMetadata>> FIELD_INDO_CACHE = new ConcurrentHashMap<>();

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
        Set<FieldMetadata> sourceFields = getFieldInfos(source.getClass());
        if (EmptyUtil.isEmpty(sourceFields)) {
            return;
        }
        Set<FieldMetadata> targetFields = getFieldInfos(target.getClass());
        if (EmptyUtil.isEmpty(targetFields)) {
            return;
        }
        Map<String, FieldMetadata> map = sourceFields.stream().collect(Collectors.toMap(FieldMetadata::getName, e -> e));
        targetFields.forEach(field -> {
            FieldMetadata sourceField = map.get(field.getName());
            if (Objects.isNull(sourceField)) {
                return;
            }
            Type sourceFieldType = sourceField.getType();
            if (!AssignableUtil.isAssignable(sourceFieldType, field.getType())) {
                return;
            }
            Function<S, Object> getter = (Function<S, Object>) sourceField.getGetter();
            BiConsumer<T, Object> setter = (BiConsumer<T, Object>) field.getSetter();
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

    private static <T> Set<FieldMetadata> getFieldInfos(Class<T> clazz) {
        if (EmptyUtil.isEmpty(clazz)) {
            return new HashSet<>();
        }
        return FIELD_INDO_CACHE.computeIfAbsent(clazz.getName(), k -> {
            List<Field> fields = ReflectUtil.getFields(clazz);
            if (EmptyUtil.isEmpty(fields)) {
                return new HashSet<>();
            }
            return fields.stream().map(f -> {
                FieldMetadata data = new FieldMetadata();
                data.setName(f.getName());
                data.setType(f.getGenericType());
                data.setGetter(LambdaUtil.buildGetter(clazz, f));
                data.setSetter(LambdaUtil.buildSetter(clazz, f));
                return data;
            }).collect(Collectors.toSet());
        });
    }
}
