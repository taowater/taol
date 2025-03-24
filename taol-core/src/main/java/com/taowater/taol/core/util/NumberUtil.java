package com.taowater.taol.core.util;

import com.taowater.taol.core.function.LambdaUtil;
import io.vavr.Function1;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 数字相关工具
 *
 * @author zhu56
 * @date 2025/03/01 15:13
 */
@UtilityClass
public class NumberUtil {

    private static final Map<Class<?>, Function<BigDecimal, ?>> TYPE_FUN = new HashMap<>();

    static {
        TYPE_FUN.put(Byte.class, BigDecimal::byteValue);
        TYPE_FUN.put(Short.class, BigDecimal::shortValue);
        TYPE_FUN.put(Integer.class, BigDecimal::intValue);
        TYPE_FUN.put(BigInteger.class, BigDecimal::toBigInteger);
        TYPE_FUN.put(Long.class, BigDecimal::longValue);
        TYPE_FUN.put(Float.class, BigDecimal::floatValue);
        TYPE_FUN.put(Double.class, BigDecimal::doubleValue);
    }

    /**
     * 获得指定类型数值
     *
     * @param bigDecimal 大小数
     * @param function   函数
     * @return {@link N}
     */
    @SuppressWarnings("unchecked")
    public <N extends Number> N getValue(BigDecimal bigDecimal, Function1<?, ? extends N> function) {
        if (EmptyUtil.isHadEmpty(bigDecimal, function)) {
            return null;
        }
        Class<? extends N> returnClass = LambdaUtil.getReturnClass(function);
        Function<BigDecimal, N> fun = (Function<BigDecimal, N>) TYPE_FUN.getOrDefault(returnClass, Function.identity());
        return fun.apply(bigDecimal);
    }

    public static BigDecimal toBigDecimal(Number number) {
        if (null == number) {
            return BigDecimal.ZERO;
        }

        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        }
        if (number instanceof Long) {
            return new BigDecimal((Long) number);
        }
        if (number instanceof Integer) {
            return new BigDecimal((Integer) number);
        }
        if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        }
        return new BigDecimal(number.toString());
    }
}
