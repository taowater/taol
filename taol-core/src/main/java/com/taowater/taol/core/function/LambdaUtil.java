package com.taowater.taol.core.function;

import lombok.experimental.UtilityClass;

import java.io.Serializable;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * λ表达式工具类
 *
 * @author 朱滔
 * @date 2021/10/10 23:42
 */

@UtilityClass
@SuppressWarnings("unchecked")
public class LambdaUtil {

    /**
     * 类型λ缓存
     */
    private static final Map<Class<?>, Method> WRITE_REPLACE_CACHE = new ConcurrentHashMap<>();

    /**
     * 返回值缓存
     */
    private static final Map<Class<?>, Class<?>> RETURN_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取方法的lambda实例
     *
     * @param fun 方法
     * @return {@link SerializedLambda}
     */
    public static <S extends Serializable> SerializedLambda getSerializedLambda(S fun) {
        if (fun == null) {
            return null;
        }
        Method method = WRITE_REPLACE_CACHE.computeIfAbsent(fun.getClass(), c -> {
            try {
                Method writeReplace = c.getDeclaredMethod("writeReplace");
                writeReplace.setAccessible(true);
                return writeReplace;
            } catch (Exception e) {
                return null;
            }
        });
        if (method == null) {
            return null;
        }
        try {
            // 每次从当前实例提取，避免缓存捕获参数造成对象长期存活或实例信息串用
            return (SerializedLambda) method.invoke(fun);
        } catch (Exception e) {
            return null;
        }
    }

    private static Class<?> getReturnClass(SerializedLambda lambda) {
        MethodType methodType = getMethodType(lambda);
        if (methodType == null) {
            return null;
        }
        return methodType.returnType();
    }

    /**
     * 获得方法返回值类型
     *
     * @param fun 方法引用
     */
    public static <S extends Serializable> Class<?> getReturnClass(S fun) {
        if (fun == null) {
            return null;
        }
        return RETURN_CACHE.computeIfAbsent(fun.getClass(), k -> getReturnClass(getSerializedLambda(fun)));
    }


    public static <R> Class<R> getReturnClass(Function0<R> fun) {
        return (Class<R>) LambdaUtil.<Function0<R>>getReturnClass(fun);
    }

    /**
     * 得到返回值的类型
     *
     * @param fun 方法
     * @return {@link Class}<{@link R}>
     */
    public static <T, R> Class<R> getReturnClass(Function1<T, R> fun) {
        return (Class<R>) LambdaUtil.<Function1<T, R>>getReturnClass(fun);
    }

    public static <T, U, R> Class<R> getReturnClass(Function2<T, U, R> fun) {
        return (Class<R>) LambdaUtil.<Function2<T, U, R>>getReturnClass(fun);
    }

    /**
     * 获取函数的参数类型
     *
     * @param fun 方法
     * @return {@link List}<{@link Class}<{@link ?}>>
     */
    public static <T, R> List<Class<?>> getParameterTypes(Function1<T, R> fun) {
        SerializedLambda lambda = getSerializedLambda(fun);
        return getParameterTypes(lambda);
    }

    public static List<Class<?>> getParameterTypes(SerializedLambda lambda) {
        if (Objects.isNull(lambda)) {
            return new ArrayList<>(0);
        }
        MethodType methodType = getMethodType(lambda);
        if (methodType == null) {
            return new ArrayList<>(0);
        }
        return Arrays.asList(methodType.parameterArray());
    }

    /**
     * 使用 JVM 方法描述符解析类型，覆盖基本类型、数组和引用类型
     */
    private static MethodType getMethodType(SerializedLambda lambda) {
        if (lambda == null) {
            return null;
        }
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = LambdaUtil.class.getClassLoader();
            }
            return MethodType.fromMethodDescriptorString(lambda.getInstantiatedMethodType(), loader);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static <T, R> List<Class<?>> getParameterTypes(Consumer2<T, R> fun) {
        SerializedLambda lambda = getSerializedLambda(fun);
        return getParameterTypes(lambda);
    }

    /**
     * 获取函数的参数类型
     *
     * @param fun 方法
     * @return {@link List}<{@link Class}<{@link ?}>>
     */
    public static <T, R> Class<?> getParameterType(Function1<T, R> fun, int index) {
        List<Class<?>> list = getParameterTypes(fun);
        return list.get(index);
    }

    /**
     * 获取函数的参数类型
     *
     * @param fun 方法
     * @return {@link List}<{@link Class}<{@link ?}>>
     */
    public static <T, T2> Class<?> getParameterType(Consumer2<T, T2> fun, int index) {
        List<Class<?>> list = getParameterTypes(fun);
        return list.get(index);
    }

}
