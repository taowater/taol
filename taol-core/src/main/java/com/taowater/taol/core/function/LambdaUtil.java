package com.taowater.taol.core.function;

import com.taowater.taol.core.reflect.ClassUtil;
import com.taowater.taol.core.reflect.MethodTypeUtil;
import com.taowater.taol.core.reflect.ReflectUtil;
import lombok.experimental.UtilityClass;

import java.io.Serializable;
import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final Map<String, SerializedLambda> CACHE = new ConcurrentHashMap<>();

    /**
     * 返回值缓存
     */
    private static final Map<Serializable, Class<?>> RETURN_CACHE = new ConcurrentHashMap<>();

    /**
     * getter 缓存
     */
    private static final Map<String, Function<?, ?>> GETTER_CACHE = new ConcurrentHashMap<>();
    /**
     * setter 的缓存
     */
    private static final Map<String, BiConsumer<?, ?>> SETTER_CACHE = new ConcurrentHashMap<>();
    /**
     * 返回类型模式
     */
    private static final Pattern RETURN_TYPE_PATTERN = Pattern.compile("\\(.*\\)L(.*);");
    /**
     * 参数类型模式
     */
    private static final Pattern PARAMETER_TYPE_PATTERN = Pattern.compile("\\((.*)\\).*");

    /**
     * 获取方法的lambda实例
     *
     * @param fun 方法
     * @return {@link SerializedLambda}
     */
    public static <S extends Serializable> SerializedLambda getSerializedLambda(S fun) {
        return CACHE.computeIfAbsent(fun.getClass().getName(), c -> {
            try {
                Method method = fun.getClass().getDeclaredMethod("writeReplace");
                method.setAccessible(true);
                return (SerializedLambda) method.invoke(fun);
            } catch (Exception e) {
                return null;
            }
        });
    }

    private static Class<?> getReturnClass(SerializedLambda lambda) {
        String expr = lambda.getInstantiatedMethodType();
        Matcher matcher = RETURN_TYPE_PATTERN.matcher(expr);
        if (!matcher.find() || matcher.groupCount() != 1) {
            return null;
        }
        return Optional.of(matcher.group(1)).map(e -> e.replace("/", ".")).map(ClassUtil::fromName).orElse(null);
    }

    /**
     * 获得方法返回值类型
     *
     * @param fun 方法引用
     */
    public static <S extends Serializable> Class<?> getReturnClass(S fun) {
        return RETURN_CACHE.computeIfAbsent(fun, k -> {
            SerializedLambda lambda = getSerializedLambda(fun);
            return getReturnClass(lambda);
        });
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
        String expr = lambda.getInstantiatedMethodType();
        Matcher matcher = PARAMETER_TYPE_PATTERN.matcher(expr);
        if (!matcher.find() || matcher.groupCount() != 1) {
            return new ArrayList<>(0);
        }
        expr = matcher.group(1);
        return Stream.of(expr.split(";"))
                .map(s -> Optional.of(s).map(e -> e.replace("L", "")).map(e -> e.replace("/", ".")).orElse(null))
                .map(ClassUtil::fromName).collect(Collectors.toList());
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
        String cacheKey = targetClass.getName() + "#" + fieldName;
        return (Function<T, R>) GETTER_CACHE.computeIfAbsent(cacheKey, k -> createGetterLambda(targetClass, fieldName, fieldType));
    }


    private static <T, R> Function<T, R> createGetterLambda(Class<T> targetClass, String fieldName, Class<R> fieldType) {
        try {
            if (fieldType == null) {
                fieldType = (Class<R>) ReflectUtil.getFieldType(targetClass, fieldName);
            }
            if (fieldType == null) {
                return null;
            }
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

            // 2. 查找方法
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.findVirtual(targetClass, getterName, MethodType.methodType(fieldType));

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
//            throw new RuntimeException("Failed to create getter for field: " + fieldName, e);
            return null;
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
        String cacheKey = targetClass.getName() + "#" + fieldName;
        return (BiConsumer<T, P>) SETTER_CACHE.computeIfAbsent(cacheKey, k -> createSetterLambda(targetClass, fieldName, fieldType));
    }

    private static <T, P> BiConsumer<T, P> createSetterLambda(Class<T> targetClass, String fieldName, Class<P> fieldType) {
        try {
            String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

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
//            throw new RuntimeException("Failed to create setter for field: " + fieldName, e);
            return null;
        }
    }
}
