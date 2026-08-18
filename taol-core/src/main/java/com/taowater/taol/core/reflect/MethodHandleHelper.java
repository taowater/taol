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
    /**
     * JDK9+ 的 {@code MethodHandles.privateLookupIn(Class, Lookup)}；用反射持有以保持 Java 8 源码/运行兼容。
     * 对 classpath（unnamed module）上的类无需 {@code --add-opens} 即可获得私有访问。
     */
    private static final Method PRIVATE_LOOKUP_IN;

    static {
        Constructor<MethodHandles.Lookup> constructor = null;
        try {
            constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);
        } catch (Throwable ignored) {
            // JDK16+ 强封装下 setAccessible 会抛 InaccessibleObjectException（RuntimeException），
            // 静态初始化必须兜住任何异常，置空后由 privilegedLookup 降级到公有 Lookup，切勿让类初始化失败。
            constructor = null;
        }
        LOOKUP_CONSTRUCTOR = constructor;

        MethodHandles.Lookup implLookup = null;
        try {
            Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            field.setAccessible(true);
            implLookup = (MethodHandles.Lookup) field.get(null);
        } catch (Throwable ignored) {
        }
        IMPL_LOOKUP = implLookup;

        Method privateLookupIn = null;
        try {
            privateLookupIn = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
        } catch (Throwable ignored) {
        }
        PRIVATE_LOOKUP_IN = privateLookupIn;
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
        // JDK9+：对 classpath（unnamed module）类无需 --add-opens 即可获得含私有权限的 Lookup。
        if (PRIVATE_LOOKUP_IN != null) {
            try {
                return (MethodHandles.Lookup) PRIVATE_LOOKUP_IN.invoke(null, clazz, MethodHandles.lookup());
            } catch (Throwable ignored) {
                // 目标类所在模块未对本模块开放等情况，继续降级。
            }
        }
        if (Modifier.isPublic(clazz.getModifiers())) {
            return MethodHandles.lookup().in(clazz);
        }
        throw new IllegalAccessException("cannot create privileged lookup for class: " + clazz.getName());
    }
}
