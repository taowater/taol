package com.taowater.core.function;

import com.taowater.taol.core.function.Function1;
import com.taowater.taol.core.function.LambdaUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LambdaUtilTest {

    @Test
    void testReturnClass() {
        Function1<String, String> fun = s -> s + "123";
        assertEquals(LambdaUtil.getReturnClass(a -> "234"), String.class);
        assertEquals(LambdaUtil.getReturnClass((a, b) -> "234"), String.class);
        assertEquals(LambdaUtil.getReturnClass(() -> new Date()), Date.class);
        assertEquals(LambdaUtil.getReturnClass(fun), String.class);
        assertEquals(LambdaUtil.getReturnClass(Object::toString), String.class);
    }

    public static Long fun2(BigDecimal d) {
        return 123L;
    }

    @Test
    void getParameterTypes() {
        Function1<String, String> fun = s -> s + "123";
//        SerFunction<String, String> fun2 = s -> s + "123";
        assertEquals(LambdaUtil.getParameterTypes(fun, 0), String.class);
        assertEquals(LambdaUtil.getParameterTypes(LambdaUtilTest::fun2, 0), BigDecimal.class);
    }
}