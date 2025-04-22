package com.taowater.taol.core.reflect;

import com.taowater.taol.core.util.CollUtil;
import com.taowater.taol.core.util.EmptyUtil;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class ReflectUtil {


    /**
     * 字段缓存
     */
    private static final Map<Class<?>, List<Field>> FIELDS_CACHE = new ConcurrentHashMap<>();

    public static List<Field> getFields(Class<?> beanClass) {
        return FIELDS_CACHE.computeIfAbsent(beanClass, k -> getFields(beanClass, true));
    }

    public static List<Field> getFields(Class<?> beanClass, boolean withSuperClassFields) {
        if (Objects.isNull(beanClass)) {
            return new ArrayList<>();
        }
        List<Field> allFields = new ArrayList<>();
        Class<?> searchType = beanClass;
        Field[] declaredFields;
        while (searchType != null) {
            declaredFields = searchType.getDeclaredFields();
            allFields.addAll(CollUtil.list(declaredFields));
            searchType = withSuperClassFields ? searchType.getSuperclass() : null;
        }
        return allFields;
    }

    public static Field getField(Class<?> beanClass, String name) {
        List<Field> fields = getFields(beanClass);
        if (EmptyUtil.isEmpty(fields)) {
            return null;
        }
        return fields.stream().filter(Objects::nonNull).filter(e -> e.getName().equals(name)).findFirst().orElse(null);
    }

    public static Class<?> getFieldType(Class<?> beanClass, String name) {
        return Optional.ofNullable(getField(beanClass, name)).map(Field::getType).orElse(null);
    }
}
