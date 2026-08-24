package com.taowater.core.util;

import com.taowater.taol.core.function.Function1;
import com.taowater.taol.core.util.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumberUtilTest {

    /**
     * 验证整数转换会拒绝小数和越界值
     */
    @Test
    void integerConversionIsExact() {
        Function1<Object, Integer> integerType = value -> 0;

        assertEquals(Integer.valueOf(12), NumberUtil.getValue(new BigDecimal("12"), integerType));
        assertThrows(ArithmeticException.class,
                () -> NumberUtil.getValue(new BigDecimal("12.5"), integerType));
        assertThrows(ArithmeticException.class,
                () -> NumberUtil.getValue(new BigDecimal("2147483648"), integerType));
    }

    /**
     * 验证浮点转换会拒绝超出目标类型范围的值
     */
    @Test
    void floatingConversionRejectsOverflow() {
        Function1<Object, Float> floatType = value -> 0F;

        assertEquals(Float.valueOf(12.5F), NumberUtil.getValue(new BigDecimal("12.5"), floatType));
        assertThrows(ArithmeticException.class,
                () -> NumberUtil.getValue(new BigDecimal("1E+1000"), floatType));
    }
}
