package com.taowater.taol.core.reflect;

import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * MethodHandle 反射工具，优先使用标准 Lookup 兼容非 public 声明类
 */
@UtilityClass
public class MethodHandleHelper {

    private static final int TRUSTED = -1;
    private static final int FULL_POWER = 31;

    /**
     * JDK9+ 的标准私有 Lookup 入口，通过反射持有以保持 Java 8 编译兼容
     */
    private static final Method PRIVATE_LOOKUP_IN;
    /**
     * Java 8 Lookup 构造器缓存
     */
    private static final Constructor<MethodHandles.Lookup> LOOKUP_CONSTRUCTOR;
    /**
     * Java 8 内部 Lookup 实例缓存
     */
    private static final MethodHandles.Lookup IMPL_LOOKUP;

    static {
        Method privateLookupIn = null;
        Constructor<MethodHandles.Lookup> lookupConstructor = null;
        MethodHandles.Lookup implLookup = null;
        try {
            privateLookupIn = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
        } catch (NoSuchMethodException ignored) {
            // Java 8 不提供标准私有 Lookup
        }
        if (privateLookupIn == null) {
            try {
                lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                lookupConstructor.setAccessible(true);
            } catch (Throwable ignored) {
                // 当前 JVM 不允许访问 Lookup 构造器时继续尝试字段 fallback
            }
            try {
                Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
                field.setAccessible(true);
                implLookup = (MethodHandles.Lookup) field.get(null);
            } catch (Throwable ignored) {
                // 当前 JVM 不允许访问内部 Lookup 时保留空缓存
            }
        }
        PRIVATE_LOOKUP_IN = privateLookupIn;
        LOOKUP_CONSTRUCTOR = lookupConstructor;
        IMPL_LOOKUP = implLookup;
    }

    @Getter
    public static final class ConstructorAccess {
        private final MethodHandles.Lookup lookup;
        private final MethodHandle handle;

        private ConstructorAccess(MethodHandles.Lookup lookup, MethodHandle handle) {
            this.lookup = lookup;
            this.handle = handle;
        }
    }

    @Getter
    public static final class MethodAccess {
        private final MethodHandles.Lookup lookup;
        private final MethodHandle handle;

        private MethodAccess(MethodHandles.Lookup lookup, MethodHandle handle) {
            this.lookup = lookup;
            this.handle = handle;
        }
    }

    public static <T> ConstructorAccess access(Constructor<T> constructor) {
        try {
            MethodHandles.Lookup lookup = standardLookup(constructor.getDeclaringClass(), constructor.getModifiers());
            return new ConstructorAccess(lookup, lookup.unreflectConstructor(constructor));
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new IllegalStateException("cannot access constructor: " + constructor, e);
        }
    }

    /**
     * 获取可在 LambdaMetafactory 中使用的 lookup 与 method handle
     */
    public static MethodAccess access(Method method) {
        try {
            MethodHandles.Lookup lookup = standardLookup(method.getDeclaringClass(), method.getModifiers());
            return new MethodAccess(lookup, lookup.unreflect(method));
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new IllegalStateException("cannot access method: " + method, e);
        }
    }

    public static MethodHandle unreflect(Method method) {
        return access(method).getHandle();
    }

    /**
     * 仅通过标准 API 获取目标类 Lookup，不绕过模块与访问边界
     */
    private static MethodHandles.Lookup standardLookup(Class<?> clazz, int memberModifiers)
            throws ReflectiveOperationException {
        if (PRIVATE_LOOKUP_IN != null) {
            try {
                return (MethodHandles.Lookup) PRIVATE_LOOKUP_IN.invoke(null, clazz, MethodHandles.lookup());
            } catch (InvocationTargetException e) {
                if (isPublicMember(clazz, memberModifiers)) {
                    return MethodHandles.lookup();
                }
                throw inaccessibleLookup(clazz, e.getCause());
            }
        }
        if (isPublicMember(clazz, memberModifiers)) {
            return MethodHandles.lookup();
        }
        // Java 8 没有 privateLookupIn，只能使用兼容旧运行时的受控 fallback
        return java8CompatibleLookup(clazz);
    }

    private static MethodHandles.Lookup java8CompatibleLookup(Class<?> clazz)
            throws ReflectiveOperationException {
        if (LOOKUP_CONSTRUCTOR != null) {
            try {
                return LOOKUP_CONSTRUCTOR.newInstance(clazz, TRUSTED);
            } catch (ReflectiveOperationException ignored) {
                // 部分 JVM 不接受 trusted 权限值，尝试兼容的 full-power 值
            }
            try {
                return LOOKUP_CONSTRUCTOR.newInstance(clazz, FULL_POWER);
            } catch (ReflectiveOperationException ignored) {
                // 当前 JVM 不接受兼容的 Lookup 权限值，继续尝试字段 fallback
            }
        }

        if (IMPL_LOOKUP != null) {
            return IMPL_LOOKUP.in(clazz);
        }
        throw new IllegalAccessException("cannot create Java 8 compatible lookup for class: " + clazz.getName());
    }

    private static boolean isPublicMember(Class<?> clazz, int memberModifiers) {
        return Modifier.isPublic(clazz.getModifiers()) && Modifier.isPublic(memberModifiers);
    }

    private static IllegalAccessException inaccessibleLookup(Class<?> clazz, Throwable cause) {
        IllegalAccessException exception = new IllegalAccessException(
                "cannot create standard private lookup for class: " + clazz.getName());
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }
}
