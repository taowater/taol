package com.taowater.core.function;

import com.taowater.taol.core.function.Function1;
import com.taowater.taol.core.function.LambdaUtil;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
        Function<TestBean, String> getter = LambdaUtil.buildGetter(TestBean.class, "name");
        assertEquals(getter.apply(testBean), testBean.getName());
    }

    @Test
    void testBuildSetter() {

        TestBean testBean = new TestBean();
        testBean.setName("test");

        String newName = "123";
        BiConsumer<TestBean, String> setter = LambdaUtil.buildSetter(TestBean.class, "name");
        setter.accept(testBean, newName);
        assertEquals(testBean.getName(), newName);
    }
}