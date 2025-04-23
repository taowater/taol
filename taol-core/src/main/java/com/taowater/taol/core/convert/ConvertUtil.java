package com.taowater.taol.core.convert;


import com.taowater.taol.core.function.LambdaUtil;
import com.taowater.taol.core.reflect.ClassUtil;
import com.taowater.taol.core.reflect.ReflectUtil;
import com.taowater.taol.core.util.EmptyUtil;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
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

    private static final Map<String, List<FieldMetadata>> FIELD_INDO_CACHE = new ConcurrentHashMap<>();

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
        List<FieldMetadata> sourceFields = getFieldInfos(source.getClass());
        if (EmptyUtil.isEmpty(sourceFields)) {
            return;
        }
        List<FieldMetadata> targetFields = getFieldInfos(target.getClass());
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
            if (!Objects.equals(sourceFieldType, field.getType())) {
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

    private static List<FieldMetadata> getFieldInfos(Class<?> clazz) {
        if (EmptyUtil.isEmpty(clazz)) {
            return new ArrayList<>();
        }
        return FIELD_INDO_CACHE.computeIfAbsent(clazz.getName(), k -> {
            List<Field> fields = ReflectUtil.getFields(clazz);
            if (EmptyUtil.isEmpty(fields)) {
                return new ArrayList<>();
            }
            return fields.stream().map(f -> {
                FieldMetadata data = new FieldMetadata();
                data.setName(f.getName());
                data.setType(f.getGenericType());
                data.setGetter(LambdaUtil.buildGetter(clazz, f));
                data.setSetter(LambdaUtil.buildSetter(clazz, f));
                return data;
            }).collect(Collectors.toList());
        });
    }
}
