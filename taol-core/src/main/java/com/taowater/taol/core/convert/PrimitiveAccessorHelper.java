package com.taowater.taol.core.convert;

import com.taowater.taol.core.function.*;
import com.taowater.taol.core.reflect.MethodHandleHelper;
import lombok.experimental.UtilityClass;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.*;

/**
 * 8 大基本类型 getter/setter 的 Lambda 构建与适配
 */
@UtilityClass
@SuppressWarnings("unchecked")
class PrimitiveAccessorHelper {

    private static final Map<Class<?>, GetterInfo> GETTER_INFO_MAP = new HashMap<>();
    private static final Map<Class<?>, SetterInfo> SETTER_INFO_MAP = new HashMap<>();

    static {
        registerGetter(boolean.class, "applyAsBoolean", ToBooleanFunction.class);
        registerGetter(byte.class, "applyAsByte", ToByteFunction.class);
        registerGetter(short.class, "applyAsShort", ToShortFunction.class);
        registerGetter(char.class, "applyAsChar", ToCharFunction.class);
        registerGetter(int.class, "applyAsInt", ToIntFunction.class);
        registerGetter(long.class, "applyAsLong", ToLongFunction.class);
        registerGetter(float.class, "applyAsFloat", ToFloatFunction.class);
        registerGetter(double.class, "applyAsDouble", ToDoubleFunction.class);

        registerSetter(boolean.class, ObjBooleanConsumer.class);
        registerSetter(byte.class, ObjByteConsumer.class);
        registerSetter(short.class, ObjShortConsumer.class);
        registerSetter(char.class, ObjCharConsumer.class);
        registerSetter(int.class, ObjIntConsumer.class);
        registerSetter(long.class, ObjLongConsumer.class);
        registerSetter(float.class, ObjFloatConsumer.class);
        registerSetter(double.class, ObjDoubleConsumer.class);
    }

    private static void registerGetter(Class<?> primitiveClass, String methodName, Class<?> functionClass) {
        GETTER_INFO_MAP.put(primitiveClass, new GetterInfo(methodName, functionClass, primitiveClass));
    }

    private static void registerSetter(Class<?> primitiveClass, Class<?> functionClass) {
        SETTER_INFO_MAP.put(primitiveClass, new SetterInfo(primitiveClass, functionClass));
    }

    static boolean isPrimitiveGetter(Class<?> returnType) {
        return GETTER_INFO_MAP.containsKey(returnType);
    }

    static boolean isPrimitiveSetter(Class<?> paramType) {
        return SETTER_INFO_MAP.containsKey(paramType);
    }

    static Object buildGetter(Class<?> targetClass, Method method) {
        GetterInfo info = GETTER_INFO_MAP.get(method.getReturnType());
        if (info == null) {
            return null;
        }
        return createGetter(targetClass, method, info);
    }

    static Object buildSetter(Class<?> targetClass, Method method, Class<?> paramType) {
        SetterInfo info = SETTER_INFO_MAP.get(paramType);
        if (info == null) {
            return null;
        }
        return createSetter(targetClass, method, paramType, info);
    }

    static Function<?, ?> asFunctionGetter(Object accessor) {
        if (accessor instanceof ToBooleanFunction) {
            return wrap((ToBooleanFunction<?>) accessor);
        }
        if (accessor instanceof ToByteFunction) {
            return wrap((ToByteFunction<?>) accessor);
        }
        if (accessor instanceof ToShortFunction) {
            return wrap((ToShortFunction<?>) accessor);
        }
        if (accessor instanceof ToCharFunction) {
            return wrap((ToCharFunction<?>) accessor);
        }
        if (accessor instanceof ToIntFunction) {
            return wrap((ToIntFunction<?>) accessor);
        }
        if (accessor instanceof ToLongFunction) {
            return wrap((ToLongFunction<?>) accessor);
        }
        if (accessor instanceof ToFloatFunction) {
            return wrap((ToFloatFunction<?>) accessor);
        }
        if (accessor instanceof ToDoubleFunction) {
            return wrap((ToDoubleFunction<?>) accessor);
        }
        return null;
    }

    static BiConsumer<?, ?> asBiConsumerSetter(Object accessor) {
        if (accessor instanceof ObjBooleanConsumer) {
            return wrap((ObjBooleanConsumer<?>) accessor);
        }
        if (accessor instanceof ObjByteConsumer) {
            return wrap((ObjByteConsumer<?>) accessor);
        }
        if (accessor instanceof ObjShortConsumer) {
            return wrap((ObjShortConsumer<?>) accessor);
        }
        if (accessor instanceof ObjCharConsumer) {
            return wrap((ObjCharConsumer<?>) accessor);
        }
        if (accessor instanceof ObjIntConsumer) {
            return wrap((ObjIntConsumer<?>) accessor);
        }
        if (accessor instanceof ObjLongConsumer) {
            return wrap((ObjLongConsumer<?>) accessor);
        }
        if (accessor instanceof ObjFloatConsumer) {
            return wrap((ObjFloatConsumer<?>) accessor);
        }
        if (accessor instanceof ObjDoubleConsumer) {
            return wrap((ObjDoubleConsumer<?>) accessor);
        }
        return null;
    }

    private static <T> Object createGetter(Class<T> targetClass, Method method, GetterInfo info) {
        try {
            MethodHandleHelper.MethodAccess access = MethodHandleHelper.access(method);
            CallSite callSite = LambdaMetafactory.metafactory(
                    access.getLookup(),
                    info.methodName,
                    MethodType.methodType(info.functionClass),
                    MethodType.methodType(info.returnClass, Object.class),
                    access.getHandle(),
                    MethodType.methodType(info.returnClass, targetClass)
            );
            return callSite.getTarget().invoke();
        } catch (Throwable e) {
            throw new CreateLambdaException(e);
        }
    }

    private static <T> Object createSetter(Class<T> targetClass, Method method, Class<?> paramType, SetterInfo info) {
        try {
            MethodHandleHelper.MethodAccess access = MethodHandleHelper.access(method);
            CallSite callSite = LambdaMetafactory.metafactory(
                    access.getLookup(),
                    "accept",
                    info.invokedType,
                    info.samMethodType,
                    access.getHandle(),
                    MethodType.methodType(void.class, targetClass, paramType)
            );
            return callSite.getTarget().invoke();
        } catch (Throwable e) {
            throw new CreateLambdaException(e);
        }
    }

    private static <T> Function<T, Object> wrap(ToBooleanFunction<T> fn) {
        return fn::applyAsBoolean;
    }

    private static <T> Function<T, Object> wrap(ToByteFunction<T> fn) {
        return fn::applyAsByte;
    }

    private static <T> Function<T, Object> wrap(ToShortFunction<T> fn) {
        return fn::applyAsShort;
    }

    private static <T> Function<T, Object> wrap(ToCharFunction<T> fn) {
        return fn::applyAsChar;
    }

    private static <T> Function<T, Object> wrap(ToIntFunction<T> fn) {
        return fn::applyAsInt;
    }

    private static <T> Function<T, Object> wrap(ToLongFunction<T> fn) {
        return fn::applyAsLong;
    }

    private static <T> Function<T, Object> wrap(ToFloatFunction<T> fn) {
        return fn::applyAsFloat;
    }

    private static <T> Function<T, Object> wrap(ToDoubleFunction<T> fn) {
        return fn::applyAsDouble;
    }

    private static <T> BiConsumer<T, Object> wrap(ObjBooleanConsumer<T> fn) {
        return (obj, val) -> fn.accept(obj, (Boolean) val);
    }

    private static <T> BiConsumer<T, Object> wrap(ObjByteConsumer<T> fn) {
        return (obj, val) -> fn.accept(obj, (Byte) val);
    }

    private static <T> BiConsumer<T, Object> wrap(ObjShortConsumer<T> fn) {
        return (obj, val) -> fn.accept(obj, (Short) val);
    }

    private static <T> BiConsumer<T, Object> wrap(ObjCharConsumer<T> fn) {
        return (obj, val) -> fn.accept(obj, (Character) val);
    }

    private static <T> BiConsumer<T, Object> wrap(ObjIntConsumer<T> fn) {
        return (obj, val) -> fn.accept(obj, (Integer) val);
    }

    private static <T> BiConsumer<T, Object> wrap(ObjLongConsumer<T> fn) {
        return (obj, val) -> fn.accept(obj, (Long) val);
    }

    private static <T> BiConsumer<T, Object> wrap(ObjFloatConsumer<T> fn) {
        return (obj, val) -> fn.accept(obj, (Float) val);
    }

    private static <T> BiConsumer<T, Object> wrap(ObjDoubleConsumer<T> fn) {
        return (obj, val) -> fn.accept(obj, (Double) val);
    }

    private static class GetterInfo {
        final String methodName;
        final Class<?> functionClass;
        final Class<?> returnClass;

        GetterInfo(String methodName, Class<?> functionClass, Class<?> returnClass) {
            this.methodName = methodName;
            this.functionClass = functionClass;
            this.returnClass = returnClass;
        }
    }

    private static class SetterInfo {
        final MethodType samMethodType;
        final MethodType invokedType;

        SetterInfo(Class<?> fieldClass, Class<?> functionClass) {
            this.samMethodType = MethodType.methodType(void.class, Object.class, fieldClass);
            this.invokedType = MethodType.methodType(functionClass);
        }
    }
}
