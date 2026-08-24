package com.taowater.core.function;

import com.taowater.taol.core.convert.GetSetHelper;
import com.taowater.taol.core.function.Function1;
import com.taowater.taol.core.function.LambdaUtil;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        assertEquals(LambdaUtil.getParameterType(fun, 0), String.class);
        assertEquals(LambdaUtil.getParameterType((Function1<BigDecimal, Long>) LambdaUtilTest::fun2, 0), BigDecimal.class);
    }

    /**
     * 验证空序列化 Lambda 返回空参数列表
     */
    @Test
    void getParameterTypesWithNullLambda() {
        assertEquals(Collections.emptyList(), LambdaUtil.getParameterTypes((java.lang.invoke.SerializedLambda) null));
        assertNull(LambdaUtil.getReturnClass((Function1<String, String>) null));
    }

    /**
     * 验证同一 Lambda 类的不同实例不会串用捕获参数
     */
    @Test
    void serializedLambdaKeepsCurrentCapturedArguments() {
        java.lang.invoke.SerializedLambda first = captured("first");
        java.lang.invoke.SerializedLambda second = captured("second");

        assertEquals("first", first.getCapturedArg(0));
        assertEquals("second", second.getCapturedArg(0));
    }

    private static java.lang.invoke.SerializedLambda captured(String suffix) {
        return LambdaUtil.getSerializedLambda((Function1<String, String>) value -> value + suffix);
    }

    @Getter
    @Setter
    public static class TestBean {
        public String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    void testBuildGetter() {

        TestBean testBean = new TestBean();
        testBean.setName("test");
        Function<TestBean, String> getter = GetSetHelper.buildGetter(TestBean.class, "name");
        assertEquals(getter.apply(testBean), testBean.getName());
    }

    @Test
    void testBuildSetter() {

        TestBean testBean = new TestBean();
        testBean.setName("test");

        String newName = "123";
        BiConsumer<TestBean, String> setter = GetSetHelper.buildSetter(TestBean.class, "name");
        Class<?> parameterTypes = LambdaUtil.getParameterType(setter::accept, 0);
        setter.accept(testBean, newName);
        assertEquals(testBean.getName(), newName);
    }
}
