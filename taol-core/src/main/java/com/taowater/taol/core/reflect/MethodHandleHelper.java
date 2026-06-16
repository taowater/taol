package com.taowater.taol.core.reflect;

import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * MethodHandle 反射工具，兼容非 public 声明类（如包级私有 Bean）
 */
@UtilityClass
public class MethodHandleHelper {

    private static final int TRUSTED = -1;
    private static final int FULL_POWER = 31;
    private static final Constructor<MethodHandles.Lookup> LOOKUP_CONSTRUCTOR;
    private static final MethodHandles.Lookup IMPL_LOOKUP;

    static {
        Constructor<MethodHandles.Lookup> constructor = null;
        try {
            constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {
            // ignored
        }
        LOOKUP_CONSTRUCTOR = constructor;

        MethodHandles.Lookup implLookup = null;
        try {
            Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            field.setAccessible(true);
            implLookup = (MethodHandles.Lookup) field.get(null);
        } catch (ReflectiveOperationException ignored) {
            // ignored
        }
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
            constructor.setAccessible(true);
            MethodHandles.Lookup lookup = privilegedLookup(constructor.getDeclaringClass());
            return new ConstructorAccess(lookup, lookup.unreflectConstructor(constructor));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot access constructor: " + constructor, e);
        }
    }

    /**
     * 获取可在 LambdaMetafactory 中使用的 lookup 与 method handle
     */
    public static MethodAccess access(Method method) {
        try {
            method.setAccessible(true);
            MethodHandles.Lookup lookup = privilegedLookup(method.getDeclaringClass());
            return new MethodAccess(lookup, lookup.unreflect(method));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot access method: " + method, e);
        }
    }

    public static MethodHandle unreflect(Method method) {
        return access(method).getHandle();
    }

    private static MethodHandles.Lookup privilegedLookup(Class<?> clazz) throws ReflectiveOperationException {
        if (LOOKUP_CONSTRUCTOR != null) {
            try {
                return LOOKUP_CONSTRUCTOR.newInstance(clazz, TRUSTED);
            } catch (ReflectiveOperationException ignored) {
                // try FULL_POWER for OpenJ9 等 JVM
            }
            return LOOKUP_CONSTRUCTOR.newInstance(clazz, FULL_POWER);
        }
        if (IMPL_LOOKUP != null) {
            try {
                return IMPL_LOOKUP.in(clazz);
            } catch (Exception ignored) {
                // fall through
            }
        }
        if (Modifier.isPublic(clazz.getModifiers())) {
            return MethodHandles.lookup().in(clazz);
        }
        throw new IllegalAccessException("cannot create privileged lookup for class: " + clazz.getName());
    }
}
