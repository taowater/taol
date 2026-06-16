package com.taowater.taol.core.convert;

import com.taowater.taol.core.reflect.MethodHandleHelper;
import com.taowater.taol.core.reflect.MethodTypeUtil;
import lombok.experimental.UtilityClass;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * get/set 构建帮助
 */
@UtilityClass
@SuppressWarnings("unchecked")
public class GetSetHelper {

    private static final MethodType METHOD_TYPE_OBJECT_GETTER =
            MethodType.methodType(Object.class, Object.class);
    private static final MethodType METHOD_TYPE_OBJECT_SETTER =
            MethodType.methodType(void.class, Object.class, Object.class);

    public static Object buildGetterAccessor(Class<?> targetClass, Method method) {
        if (method == null) {
            return null;
        }
        if (PrimitiveAccessorHelper.isPrimitiveGetter(method.getReturnType())) {
            return PrimitiveAccessorHelper.buildGetter(targetClass, method);
        }
        return createObjectGetter(targetClass, method);
    }

    public static Object buildSetterAccessor(Class<?> targetClass, Method method) {
        if (method == null) {
            return null;
        }
        Class<?> paramType = method.getParameterTypes()[0];
        if (PrimitiveAccessorHelper.isPrimitiveSetter(paramType)) {
            return PrimitiveAccessorHelper.buildSetter(targetClass, method, paramType);
        }
        return createObjectSetter(targetClass, method, paramType);
    }

    public static <T, R> Function<T, R> buildGetter(Class<T> targetClass, Field field) {
        if (Objects.isNull(field)) {
            return null;
        }
        return buildGetter(targetClass, field.getName());
    }

    public static <T, R> Function<T, R> buildGetter(Class<T> targetClass, String fieldName) {
        return buildGetter(targetClass, fieldName, null);
    }

    public static <T, R> Function<T, R> buildGetter(Class<T> targetClass, String fieldName, Class<R> fieldType) {
        Method method = BeanMethodResolver.resolveGetter(targetClass, fieldName);
        if (method == null) {
            return null;
        }
        return (Function<T, R>) asFunctionGetter(buildGetterAccessor(targetClass, method));
    }

    public static <T, P> BiConsumer<T, P> buildSetter(Class<T> targetClass, Field field) {
        if (Objects.isNull(field)) {
            return null;
        }
        return buildSetter(targetClass, field.getName());
    }

    public static <T, P> BiConsumer<T, P> buildSetter(Class<T> targetClass, String fieldName) {
        return buildSetter(targetClass, fieldName, null);
    }

    public static <T, P> BiConsumer<T, P> buildSetter(Class<T> targetClass, String fieldName, Class<P> fieldType) {
        Method method = BeanMethodResolver.resolveSetter(targetClass, fieldName);
        if (method == null) {
            return null;
        }
        return (BiConsumer<T, P>) asBiConsumerSetter(buildSetterAccessor(targetClass, method));
    }

    public static Function<?, ?> asFunctionGetter(Object accessor) {
        if (accessor == null) {
            return null;
        }
        if (accessor instanceof Function) {
            return (Function<?, ?>) accessor;
        }
        Function<?, ?> primitiveGetter = PrimitiveAccessorHelper.asFunctionGetter(accessor);
        if (primitiveGetter != null) {
            return primitiveGetter;
        }
        throw new CreateLambdaException("unsupported getter accessor: " + accessor.getClass());
    }

    public static BiConsumer<?, ?> asBiConsumerSetter(Object accessor) {
        if (accessor == null) {
            return null;
        }
        if (accessor instanceof BiConsumer) {
            return (BiConsumer<?, ?>) accessor;
        }
        BiConsumer<?, ?> primitiveSetter = PrimitiveAccessorHelper.asBiConsumerSetter(accessor);
        if (primitiveSetter != null) {
            return primitiveSetter;
        }
        throw new CreateLambdaException("unsupported setter accessor: " + accessor.getClass());
    }

    private static <T> Function<T, ?> createObjectGetter(Class<T> targetClass, Method method) {
        try {
            MethodHandleHelper.MethodAccess access = MethodHandleHelper.access(method);
            MethodType instantiatedMethodType = MethodType.methodType(method.getReturnType(), targetClass);
            CallSite callSite = LambdaMetafactory.metafactory(
                    access.getLookup(),
                    "apply",
                    MethodType.methodType(Function.class),
                    METHOD_TYPE_OBJECT_GETTER,
                    access.getHandle(),
                    instantiatedMethodType
            );
            return (Function<T, ?>) callSite.getTarget().invoke();
        } catch (Throwable e) {
            throw new CreateLambdaException(e);
        }
    }

    private static <T, P> BiConsumer<T, P> createObjectSetter(Class<T> targetClass, Method method, Class<P> paramType) {
        try {
            MethodType methodType = method.getReturnType() == void.class
                    ? MethodTypeUtil.voidReturnType(paramType)
                    : MethodType.methodType(method.getReturnType(), paramType);
            MethodHandleHelper.MethodAccess access = MethodHandleHelper.access(method);
            MethodHandle handle = access.getHandle();
            if (!handle.type().equals(MethodType.methodType(method.getReturnType(), targetClass, paramType))) {
                method.setAccessible(true);
                handle = access.getLookup().findVirtual(targetClass, method.getName(), methodType);
            }
            CallSite callSite = LambdaMetafactory.metafactory(
                    access.getLookup(),
                    "accept",
                    MethodType.methodType(BiConsumer.class),
                    METHOD_TYPE_OBJECT_SETTER,
                    handle,
                    MethodType.methodType(void.class, targetClass, paramType)
            );
            return (BiConsumer<T, P>) callSite.getTarget().invoke();
        } catch (Throwable e) {
            throw new CreateLambdaException(e);
        }
    }
}
