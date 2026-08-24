package com.taowater.taol.core.util;

import com.taowater.taol.core.function.Function1;
import com.taowater.taol.core.function.LambdaUtil;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
        TYPE_FUN.put(Byte.class, BigDecimal::byteValueExact);
        TYPE_FUN.put(Short.class, BigDecimal::shortValueExact);
        TYPE_FUN.put(Integer.class, BigDecimal::intValueExact);
        TYPE_FUN.put(BigInteger.class, BigDecimal::toBigIntegerExact);
        TYPE_FUN.put(Long.class, BigDecimal::longValueExact);
        TYPE_FUN.put(Float.class, NumberUtil::toFiniteFloat);
        TYPE_FUN.put(Double.class, NumberUtil::toFiniteDouble);
        TYPE_FUN.put(BigDecimal.class, Function.identity());
        TYPE_FUN.put(Number.class, Function.identity());
    }

    /**
     * 获得指定类型数值
     *
     * @param bigDecimal 大小数
     * @param function   函数
     * @return {@link N}
     */
    @SuppressWarnings("unchecked")
    public static <N extends Number> N getValue(BigDecimal bigDecimal, Function1<?, ? extends N> function) {
        if (Objects.isNull(bigDecimal) || Objects.isNull(function)) {
            return null;
        }
        Class<? extends N> returnClass = LambdaUtil.getReturnClass(function);
        Function<BigDecimal, ?> converter = TYPE_FUN.get(returnClass);
        if (converter == null) {
            throw new IllegalArgumentException("unsupported numeric return type: " + returnClass);
        }
        return (N) converter.apply(bigDecimal);
    }

    /**
     * 转换为有限 float，溢出时保持与 Bean 转换一致的失败语义
     */
    private static Float toFiniteFloat(BigDecimal value) {
        float result = value.floatValue();
        if (Float.isInfinite(result)) {
            throw new ArithmeticException("numeric value overflows float: " + value);
        }
        return result;
    }

    /**
     * 转换为有限 double，溢出时保持与 Bean 转换一致的失败语义
     */
    private static Double toFiniteDouble(BigDecimal value) {
        double result = value.doubleValue();
        if (Double.isInfinite(result)) {
            throw new ArithmeticException("numeric value overflows double: " + value);
        }
        return result;
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
