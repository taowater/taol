package com.taowater.taol.core.reflect;

import lombok.experimental.UtilityClass;

import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


/**
 * class工具类
 *
 * @author 朱滔
 * @date 2021/9/19 15:35
 */
@UtilityClass
public class ClassUtil {


    private static final Map<Class<?>, Supplier<?>> CACHE = new ConcurrentHashMap<>();

    /**
     * 根据类型创建实例
     *
     * @param clazz clazz
     * @return {@link T}
     */
    public static <T> T newInstance(Class<T> clazz) {
        Supplier<T> supplier = getConstructor(clazz);
        return supplier.get();
    }

    /**
     * 获取空参构造器
     *
     * @param clazz 类型
     *
     */
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> getConstructor(Class<T> clazz) {

        return (Supplier<T>) CACHE.computeIfAbsent(clazz, cls -> {
            try {
                Constructor<T> cons = clazz.getDeclaredConstructor();
                cons.setAccessible(true);

                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandle constructorHandle = lookup.unreflectConstructor(cons);

                CallSite site = LambdaMetafactory.metafactory(
                        lookup,
                        "get",
                        MethodType.methodType(Supplier.class),
                        MethodType.methodType(Object.class),
                        constructorHandle,
                        MethodType.methodType(clazz)
                );
                return (Supplier<T>) site.getTarget().invokeExact();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });

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

    /**
     * 获取字段get方法名称
     */
    public static String getGetMethodName(String fieldName) {
        return "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    /**
     * 获取原生boolean-get方法名
     */
    public static String getPrimitiveBooleanGetMethodName(String fieldName) {
        return "is" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    public static String getSetMethodName(String fieldName) {
        return "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }
}
