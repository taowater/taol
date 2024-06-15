package io.github.taowater.core.function;

import io.github.taowater.core.reflect.ClassUtil;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.var;
import org.dromara.hutool.core.reflect.method.MethodUtil;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.util.*;
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
    private final Map<String, SerializedLambda> CACHE = new ConcurrentHashMap<>();

    /**
     * 返回类型模式
     */
    private final Pattern RETURN_TYPE_PATTERN = Pattern.compile("\\(.*\\)L(.*);");
    /**
     * 参数类型模式
     */
    private final Pattern PARAMETER_TYPE_PATTERN = Pattern.compile("\\((.*)\\).*");

    /**
     * 获取方法的lambda实例
     *
     * @param fun 方法
     * @return {@link SerializedLambda}
     */
    @SneakyThrows
    public <S extends Serializable> SerializedLambda getSerializedLambda(S fun) {
        return CACHE.computeIfAbsent(fun.getClass().getName(), c -> MethodUtil.invoke(fun, "writeReplace"));
    }

    /**
     * 得到返回值的类型
     *
     * @param fun 有趣
     * @return {@link Class}<{@link R}>
     */
    public <T, R> Class<R> getReturnClass(SerFunction<T, R> fun) {
        SerializedLambda lambda = getSerializedLambda(fun);
        return (Class<R>) getReturnClass(lambda);
    }

    public static <T, U, R> Class<R> getReturnClass(SerBiFunction<T, U, R> fun) {
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

    @SneakyThrows
    public static <T> Class<T> getReturnClass(SerSupplier<T> fun) {
        SerializedLambda lambda = getSerializedLambda(fun);
        return (Class<T>) getReturnClass(lambda);
    }

    /**
     * 获取函数的参数类型
     *
     * @param fun 方法
     * @return {@link List}<{@link Class}<{@link ?}>>
     */
    @SneakyThrows
    public <T, R> List<Class<?>> getParameterTypes(SerFunction<T, R> fun) {
        SerializedLambda lambda = getSerializedLambda(fun);
        String expr = lambda.getInstantiatedMethodType();
        Matcher matcher = PARAMETER_TYPE_PATTERN.matcher(expr);
        if (!matcher.find() || matcher.groupCount() != 1) {
            return new ArrayList<>(0);
        }
        expr = matcher.group(1);
        return Stream.of(expr.split(";"))
                .filter(Objects::nonNull)
                .map(s -> Optional.of(s).map(e -> e.replace("L", "")).map(e -> e.replace("/", ".")).orElse(null))
                .map(ClassUtil::fromName).collect(Collectors.toList());
    }

    /**
     * 获取函数的参数类型
     *
     * @param fun 方法
     * @return {@link List}<{@link Class}<{@link ?}>>
     */
    @SneakyThrows
    public <T, R> Class<?> getParameterTypes(SerFunction<T, R> fun, int index) {
        var list = getParameterTypes(fun);
        return list.get(index);
    }
}
