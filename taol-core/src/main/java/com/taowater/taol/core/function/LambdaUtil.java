package com.taowater.taol.core.function;

import com.taowater.taol.core.reflect.ClassUtil;
import com.taowater.taol.core.reflect.ReflectUtil;
import lombok.experimental.UtilityClass;

import java.io.Serializable;
import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
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

    /**
     * 得到返回值的类型
     *
     * @param fun 有趣
     * @return {@link Class}<{@link R}>
     */
    public static <T, R> Class<R> getReturnClass(Function1<T, R> fun) {
        SerializedLambda lambda = getSerializedLambda(fun);
        return (Class<R>) getReturnClass(lambda);
    }

    public static <T, U, R> Class<R> getReturnClass(Function2<T, U, R> fun) {
        SerializedLambda lambda = getSerializedLambda(fun);
        return (Class<R>) getReturnClass(lambda);
    }

    private static Class<?> getReturnClass(SerializedLambda lambda) {
        String expr = lambda.getInstantiatedMethodType();
        Matcher matcher = RETURN_TYPE_PATTERN.matcher(expr);
        if (!matcher.find() || matcher.groupCount() != 1) {
            return null;
        }
        return Optional.of(matcher.group(1)).map(e -> e.replace("/", ".")).map(ClassUtil::fromName).orElse(null);
    }


    public static <T> Class<T> getReturnClass(Function0<T> fun) {
        SerializedLambda lambda = getSerializedLambda(fun);
        return (Class<T>) getReturnClass(lambda);
    }

    /**
     * 获取函数的参数类型
     *
     * @param fun 方法
     * @return {@link List}<{@link Class}<{@link ?}>>
     */
    public static <T, R> List<Class<?>> getParameterTypes(Function1<T, R> fun) {
        SerializedLambda lambda = getSerializedLambda(fun);
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

    /**
     * 获取函数的参数类型
     *
     * @param fun 方法
     * @return {@link List}<{@link Class}<{@link ?}>>
     */
    public static <T, R> Class<?> getParameterTypes(Function1<T, R> fun, int index) {
        List<Class<?>> list = getParameterTypes(fun);
        return list.get(index);
    }

    /**
     * 动态构建Getter Lambda
     */
    @SuppressWarnings("unchecked")
    public static <T, R> Function<T, R> buildGetter(Class<T> targetClass, String fieldName) {
        String cacheKey = targetClass.getName() + "#" + fieldName;
        return (Function<T, R>) GETTER_CACHE.computeIfAbsent(cacheKey, k -> createGetterLambda(targetClass, fieldName));
    }

    private static <T, R> Function<T, R> createGetterLambda(Class<T> targetClass, String fieldName) {
        try {
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

            Class<?> fieldType = ReflectUtil.getFieldType(targetClass, fieldName);
            if (fieldType == null) {
                return null;
            }
            // 2. 查找方法
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.findVirtual(targetClass, getterName, MethodType.methodType(fieldType));

            // 3. 生成Lambda
            CallSite callSite = LambdaMetafactory.metafactory(
                    lookup,
                    "apply",
                    MethodType.methodType(Function.class),
                    MethodType.methodType(Object.class, Object.class),
                    handle,
                    MethodType.methodType(fieldType, targetClass)
            );

            return (Function<T, R>) callSite.getTarget().invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create getter for field: " + fieldName, e);
        }
    }

    /**
     * 动态构建Setter Lambda
     */
    @SuppressWarnings("unchecked")
    public static <T, P> BiConsumer<T, P> buildSetter(Class<T> targetClass, String fieldName) {
        String cacheKey = targetClass.getName() + "#" + fieldName;
        return (BiConsumer<T, P>) SETTER_CACHE.computeIfAbsent(cacheKey, k -> createSetterLambda(targetClass, fieldName));
    }

    private static <T, P> BiConsumer<T, P> createSetterLambda(Class<T> targetClass, String fieldName) {
        try {
            String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Class<?> fieldType = ReflectUtil.getFieldType(targetClass, fieldName);
            if (fieldType == null) {
                return null;
            }
            // 2. 查找方法
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.findVirtual(targetClass, setterName, MethodType.methodType(void.class, fieldType));

            // 3. 生成Lambda
            CallSite callSite = LambdaMetafactory.metafactory(
                    lookup,
                    "accept",
                    MethodType.methodType(BiConsumer.class),
                    MethodType.methodType(void.class, Object.class, Object.class),
                    handle,
                    MethodType.methodType(void.class, targetClass, fieldType)
            );

            return (BiConsumer<T, P>) callSite.getTarget().invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create setter for field: " + fieldName, e);
        }
    }
}
