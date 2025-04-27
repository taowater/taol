package com.taowater.taol.core.reflect;

import lombok.experimental.UtilityClass;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 类型 util
 *
 * @author zhu56
 */
@UtilityClass
public class TypeUtil {


    /**
     * 获取 Type 的原始类型（如 List<Integer> → List）
     */
    public static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        if (type instanceof GenericArrayType) {
            return Object[].class;
        }
        return null;
    }

    /**
     * 获取数组的组件类型（如 Integer[] → Integer）
     */
    public static Type getArrayComponentType(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            return clazz.isArray() ? clazz.getComponentType() : null;
        }
        if (type instanceof GenericArrayType) {
            return ((GenericArrayType) type).getGenericComponentType();
        }
        return null;
    }
}
