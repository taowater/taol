package com.taowater.taol.core.convert;

import com.taowater.taol.core.reflect.ReflectUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * bean元信息
 */
@Getter
@EqualsAndHashCode
public class BeanMetadata {

    private static final Map<Class<?>, BeanMetadata> CACHE = new ConcurrentHashMap<>();

    @Setter
    private Class<?> clazz;

    private final Map<String, FieldMetadata> fieldMap = new LinkedHashMap<>();

    public static BeanMetadata of(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, k -> new BeanMetadata(clazz));
    }

    public BeanMetadata(final Class<?> clazz) {
        this.clazz = clazz;
        List<Field> fields = ReflectUtil.getFields(clazz);
        if (Objects.isNull(fields)) {
            return;
        }
        fields.forEach(f -> {
            FieldMetadata data = new FieldMetadata();
            data.setName(f.getName());
            data.setType(f.getGenericType());
            data.setFieldClass(f.getType());
            data.setGetterMethod(BeanMethodResolver.resolveGetter(clazz, f));
            data.setSetterMethod(BeanMethodResolver.resolveSetter(clazz, f));
            fieldMap.putIfAbsent(f.getName(), data);
        });
    }

    public FieldMetadata getField(String name) {
        return fieldMap.get(name);
    }

    public Function<?, ?> getGetter(String fieldName) {
        FieldMetadata data = fieldMap.get(fieldName);
        if (data == null) {
            return null;
        }
        return data.asFunctionGetter(clazz);
    }

    public BiConsumer<?, ?> getSetter(String fieldName) {
        FieldMetadata data = fieldMap.get(fieldName);
        if (data == null) {
            return null;
        }
        return data.asBiConsumerSetter(clazz);
    }
}
