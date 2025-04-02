package com.taowater.taol.core.reflect;

import lombok.experimental.UtilityClass;

import java.lang.reflect.*;
import java.util.Optional;


/**
 * class工具类
 *
 * @author 朱滔
 * @date 2021/9/19 15:35
 */
@UtilityClass
public class ClassUtil {

    /**
     * 根据类型创建实例
     *
     * @param clazz clazz
     * @return {@link T}
     */
    public static <T> T newInstance(Class<T> clazz) {
        return Optional.of(clazz).map(c -> {
            try {
                return c.getConstructor();
            } catch (NoSuchMethodException e) {
                return null;
            }
        }).map(c -> {
            try {
                return c.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * 创建带斜杠的全限定类名
     *
     * @param clazz clazz
     * @return {@link String}
     */
    public static String getSlashName(Class<?> clazz) {
        return clazz.getName().replaceAll("\\.", "/");
    }

    public static Class<?> fromName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Class<?> getClass(final Type type) {
        if (type == null) {
            return null;
        }
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        if (type instanceof TypeVariable) {
            final Type[] bounds = ((TypeVariable<?>) type).getBounds();
            if (bounds.length == 1) {
                return getClass(bounds[0]);
            }
        } else if (type instanceof WildcardType) {
            final Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            if (upperBounds.length == 1) {
                return getClass(upperBounds[0]);
            }
        }
        return null;
    }
}
