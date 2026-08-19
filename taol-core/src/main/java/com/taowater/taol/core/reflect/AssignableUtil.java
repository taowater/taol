package com.taowater.taol.core.reflect;

import lombok.experimental.UtilityClass;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 判断赋值工具
 *
 * @author zhu56
 */
@UtilityClass
public class AssignableUtil {

    private static final Map<Class<?>, Class<?>> WRAPPER = new HashMap<>();
    private static final Map<String, Boolean> RESULT = new ConcurrentHashMap<>();

    static {
        WRAPPER.put(int.class, Integer.class);
        WRAPPER.put(long.class, Long.class);
        WRAPPER.put(double.class, Double.class);
        WRAPPER.put(float.class, Float.class);
        WRAPPER.put(boolean.class, Boolean.class);
        WRAPPER.put(byte.class, Byte.class);
        WRAPPER.put(short.class, Short.class);
        WRAPPER.put(char.class, Character.class);
    }


    /**
     * 获取包装类对应的基本类型（如果没有则返回 null）
     */
    private static Class<?> getPrimitive(Class<?> wrapperType) {
        for (Map.Entry<Class<?>, Class<?>> entry : WRAPPER.entrySet()) {
            if (entry.getValue().equals(wrapperType)) {
                return entry.getKey();
            }
        }
        return null;
    }


    /**
     * 判断两个类型是否可以直接赋值（包括泛型、自动装箱/拆箱、继承关系）
     */
    public static boolean isAssignable(Type source, Type target) {
        if (Objects.isNull(source) || Objects.isNull(target)) {
            return false;
        }
        String key = source.getTypeName() + "@" + target.getTypeName();
        Boolean cached = RESULT.get(key);
        if (Objects.nonNull(cached)) {
            return cached;
        }

        boolean result;
        if (source.equals(target)) {
            result = true;
        } else if (source instanceof Class<?> && target instanceof Class<?>) {
            result = isClassAssignable((Class<?>) source, (Class<?>) target);
        } else if (source instanceof ParameterizedType || target instanceof ParameterizedType) {
            // 泛型类型
            result = isGenericAssignable(source, target);
        } else if (source instanceof GenericArrayType || target instanceof GenericArrayType) {
            // 数组类型
            result = isArrayAssignable(source, target);
        } else {
            result = false;
        }
        // 递归计算完成后再写入，避免修改正在执行的缓存计算
        RESULT.put(key, result);
        return result;
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
            targetWrapper = WRAPPER.get(target);
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
