package com.taowater.taol.core.convert;

import com.taowater.taol.core.reflect.ClassUtil;
import com.taowater.taol.core.reflect.MethodTypeUtil;
import com.taowater.taol.core.reflect.ReflectUtil;
import lombok.experimental.UtilityClass;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * get/set构建帮助
 *
 * @author zhu56
 */
@UtilityClass
@SuppressWarnings("unchecked")
public class GetSetHelper {


    public static <T, R> Function<T, R> buildGetter(Class<T> targetClass, Field field) {
        if (Objects.isNull(field)) {
            return null;
        }
        return buildGetter(targetClass, field.getName(), (Class<R>) field.getType());
    }

    /**
     * 动态构建Getter Lambda
     */
    public static <T, R> Function<T, R> buildGetter(Class<T> targetClass, String fieldName) {
        return buildGetter(targetClass, fieldName, null);
    }

    public static <T, R> Function<T, R> buildGetter(Class<T> targetClass, String fieldName, Class<R> fieldType) {
        return createGetterLambda(targetClass, fieldName, fieldType);
    }


    private static <T, R> Function<T, R> createGetterLambda(Class<T> targetClass, String fieldName, Class<R> fieldType) {
        try {
            if (fieldType == null) {
                fieldType = (Class<R>) ReflectUtil.getFieldType(targetClass, fieldName);
            }

            if (fieldType == null) {
                return null;
            }

            String getterName = ClassUtil.getGetMethodName(fieldName);
            // 2. 查找方法
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = null;
            try {
                handle = lookup.findVirtual(targetClass, getterName, MethodType.methodType(fieldType));
            } catch (NoSuchMethodException noSuchMethodException) {
                // 如果是离谱的原生布尔类型，兼容lombok的isXX
                if (fieldType.equals(boolean.class)) {
                    getterName = ClassUtil.getPrimitiveBooleanGetMethodName(fieldName);
                    handle = lookup.findVirtual(targetClass, getterName, MethodType.methodType(fieldType));
                } else {
                    throw noSuchMethodException;
                }
            }

            // 3. 生成Lambda
            CallSite callSite = LambdaMetafactory.metafactory(
                    lookup,
                    "apply",
                    MethodType.methodType(Function.class),
                    MethodTypeUtil.functionType(1),
                    handle,
                    MethodType.methodType(fieldType, targetClass)
            );
            return (Function<T, R>) callSite.getTarget().invokeExact();
        } catch (Throwable e) {
            throw new CreateLambdaException(e);
        }
    }

    public static <T, P> BiConsumer<T, P> buildSetter(Class<T> targetClass, Field field) {
        if (Objects.isNull(field)) {
            return null;
        }
        return buildSetter(targetClass, field.getName(), (Class<P>) field.getType());
    }

    public static <T, P> BiConsumer<T, P> buildSetter(Class<T> targetClass, String fieldName) {
        return buildSetter(targetClass, fieldName, null);
    }

    /**
     * 动态构建Setter Lambda
     */
    @SuppressWarnings("unchecked")
    public static <T, P> BiConsumer<T, P> buildSetter(Class<T> targetClass, String fieldName, Class<P> fieldType) {
        return createSetterLambda(targetClass, fieldName, fieldType);
    }

    private static <T, P> BiConsumer<T, P> createSetterLambda(Class<T> targetClass, String fieldName, Class<P> fieldType) {
        try {
            String setterName = ClassUtil.getSetMethodName(fieldName);

            if (fieldType == null) {
                fieldType = (Class<P>) ReflectUtil.getFieldType(targetClass, fieldName);
            }
            if (fieldType == null) {
                return null;
            }
            boolean chain = false;
            MethodHandle handle;
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            try {
                handle = lookup.findVirtual(targetClass, setterName, MethodTypeUtil.voidReturnType(fieldType));
            } catch (NoSuchMethodException e) {
                // 如果找不到普通setter，尝试查找链式调用的setter
                handle = lookup.findVirtual(targetClass, setterName, MethodType.methodType(targetClass, fieldType));
                chain = true;
            }
            if (handle == null) {
                return null;
            }


            CallSite callSite = chain ?
                    LambdaMetafactory.metafactory(
                            lookup,
                            "apply",
                            MethodType.methodType(BiFunction.class),
                            MethodTypeUtil.functionType(2),
                            handle,
                            MethodType.methodType(targetClass, targetClass, fieldType)
                    ) :
                    LambdaMetafactory.metafactory(
                            lookup,
                            "accept",
                            MethodType.methodType(BiConsumer.class),
                            MethodTypeUtil.consumerType(2),
                            handle,
                            MethodTypeUtil.voidReturnType(targetClass, fieldType)
                    );

            if (chain) {
                return ((BiFunction<T, P, T>) callSite.getTarget().invokeExact())::apply;
            }
            return (BiConsumer<T, P>) callSite.getTarget().invokeExact();
        } catch (Throwable e) {
            throw new CreateLambdaException(e);
        }
    }
}
