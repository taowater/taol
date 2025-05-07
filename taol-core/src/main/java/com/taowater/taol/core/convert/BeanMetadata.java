package com.taowater.taol.core.convert;

import com.taowater.taol.core.reflect.ReflectUtil;
import com.taowater.taol.core.util.EmptyUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * bean元信息
 *
 * @author zhu56
 */
@Getter
@EqualsAndHashCode
public class BeanMetadata {

    /**
     * 类的全限定类名与类的字段信息缓存
     */
    private static final Map<Class<?>, BeanMetadata> CACHE = new ConcurrentHashMap<>();
    @Setter
    private Class<?> clazz;
    /**
     * 属性Map
     */
    private final Map<String, FieldMetadata> fieldMap = new LinkedHashMap<>();

    public static BeanMetadata of(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, k -> new BeanMetadata(clazz));
    }

    public BeanMetadata(final Class<?> clazz) {
        this.clazz = clazz;
        List<Field> fields = ReflectUtil.getFields(clazz);
        if (EmptyUtil.isEmpty(fields)) {
            return;
        }
        fields.forEach(f -> {
            FieldMetadata data = new FieldMetadata();
            data.setName(f.getName());
            data.setType(f.getGenericType());
            fieldMap.putIfAbsent(f.getName(), data);
        });
    }

    public FieldMetadata getField(String name) {
        return fieldMap.get(name);
    }

    public Function<?, ?> getGetter(String fieldName) {
        FieldMetadata data = fieldMap.get(fieldName);
        if (data.getGetter() == null) {
            data.setGetter(GetSetHelper.buildGetter(clazz, fieldName));
        }
        return data.getGetter();
    }

    public BiConsumer<?, ?> getSetter(String fieldName) {
        FieldMetadata data = fieldMap.get(fieldName);
        if (data.getSetter() == null) {
            data.setSetter(GetSetHelper.buildSetter(clazz, fieldName));
        }
        return data.getSetter();
    }
}
