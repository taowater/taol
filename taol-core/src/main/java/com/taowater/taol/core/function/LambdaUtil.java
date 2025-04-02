package com.taowater.taol.core.function;

import com.taowater.taol.core.reflect.ClassUtil;
import lombok.experimental.UtilityClass;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
}
