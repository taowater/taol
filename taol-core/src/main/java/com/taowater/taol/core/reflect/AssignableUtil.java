package com.taowater.taol.core.reflect;

import lombok.experimental.UtilityClass;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 判断赋值工具
 *
 * @author zhu56
 */
@UtilityClass
public class AssignableUtil {

    private static final Map<Class<?>, Class<?>> WRAPPER = new HashMap<>();
    private static final Map<String, Boolean> RESULT = new HashMap<>();
    public static final Map<Class<?>, Map<Class<?>, Function<?, ?>>> CONVERT = new HashMap<>();

    static {
        WRAPPER.put(int.class, Integer.class);
        WRAPPER.put(long.class, Long.class);
        WRAPPER.put(double.class, Double.class);
        WRAPPER.put(float.class, Float.class);
        WRAPPER.put(boolean.class, Boolean.class);
        WRAPPER.put(byte.class, Byte.class);
        WRAPPER.put(short.class, Short.class);
        WRAPPER.put(char.class, Character.class);


        Map<Class<?>, Function<?, ?>> map = new HashMap<>();
        map.put(Byte.class, (Integer e) -> e.byteValue());
        map.put(Short.class, (Integer e) -> e.shortValue());
        map.put(Float.class, (Integer e) -> e.floatValue());
        map.put(Double.class, (Integer e) -> e.doubleValue());
        map.put(Long.class, (Integer e) -> e.longValue());
        CONVERT.put(Integer.class, map);
    }


    /**
     * 获取包装类对应的基本类型（如果没有则返回 null）
     */
    private static Class<?> getPrimitive(Class<?> wrapperType) {
        return WRAPPER.entrySet().stream()
                .filter(e -> e.getValue().equals(wrapperType))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(null);
    }


    /**
     * 判断两个类型是否可以直接赋值（包括泛型、自动装箱/拆箱、继承关系）
     */
    public static boolean isAssignable(Type source, Type target) {
        if (Objects.isNull(source) || Objects.isNull(target)) {
            return false;
        }
        return RESULT.computeIfAbsent(source.getTypeName() + "@" + target.getTypeName(), k -> {
            if (source.equals(target)) {
                return true;
            }

            if (source instanceof Class<?> && target instanceof Class<?>) {
                return isClassAssignable((Class<?>) source, (Class<?>) target);
            }

            // 泛型类型
            if (source instanceof ParameterizedType || target instanceof ParameterizedType) {
                return isGenericAssignable(source, target);
            }

            // 数组类型
            if (source instanceof GenericArrayType || target instanceof GenericArrayType) {
                return isArrayAssignable(source, target);
            }
            return false;
        });
    }


    /**
     * 判断两个 Class 是否可以直接赋值（基本类型、包装类型、继承关系）
     */
    private static boolean isClassAssignable(Class<?> source, Class<?> target) {
        Class<?> sourceWrapper = source;
        if (source.isPrimitive()) {
            sourceWrapper = WRAPPER.get(source);
        }
        Class<?> targetWrapper = target;
        if (target.isPrimitive()) {
            targetWrapper = getPrimitive(target);
        }
        return Objects.equals(sourceWrapper, targetWrapper) || targetWrapper.isAssignableFrom(sourceWrapper);
    }

    /**
     * 判断泛型类型是否可以直接赋值（如 List<Integer> 和 List<Number>）
     */
    private static boolean isGenericAssignable(Type source, Type target) {
        Class<?> sourceRawType = TypeUtil.getRawType(source);
        Class<?> targetRawType = TypeUtil.getRawType(target);

        if (!isClassAssignable(sourceRawType, targetRawType)) {
            return false;
        }

        // 比较泛型参数
        Type[] fromTypeArgs = ((ParameterizedType) source).getActualTypeArguments();
        Type[] toTypeArgs = ((ParameterizedType) target).getActualTypeArguments();

        if (fromTypeArgs.length != toTypeArgs.length) {
            return false;
        }

        // 泛型参数是否兼容
        for (int i = 0; i < fromTypeArgs.length; i++) {
            if (!isAssignable(fromTypeArgs[i], toTypeArgs[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断数组类型是否可以直接赋值（如 Integer[] 和 Number[]）
     */
    private static boolean isArrayAssignable(Type source, Type target) {
        Type sourceComponentType = TypeUtil.getArrayComponentType(source);
        Type targetComponentType = TypeUtil.getArrayComponentType(target);
        return isAssignable(sourceComponentType, targetComponentType);
    }
}