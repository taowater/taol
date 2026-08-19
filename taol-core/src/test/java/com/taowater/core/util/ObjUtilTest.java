package com.taowater.core.util;

import com.taowater.taol.core.util.ObjUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ObjUtilTest {

    /**
     * 验证对象存在时执行映射并返回转换结果
     */
    @Test
    void testGetWithValue() {
        AtomicInteger calls = new AtomicInteger();

        String result = ObjUtil.get(123, value -> {
            calls.incrementAndGet();
            return String.valueOf(value);
        });

        assertEquals("123", result);
        assertEquals(1, calls.get());
    }

    /**
     * 验证对象为空时不执行映射并返回 null
     */
    @Test
    void testGetWithNullValue() {
        AtomicInteger calls = new AtomicInteger();

        String result = ObjUtil.get(null, value -> {
            calls.incrementAndGet();
            return String.valueOf(value);
        });

        assertNull(result);
        assertEquals(0, calls.get());
    }

    /**
     * 验证 get 对空对象使用异常供应器
     */
    @Test
    void testGetWithNullValueAndThrowSupplier() {
        AtomicInteger mapperCalls = new AtomicInteger();
        AtomicInteger supplierCalls = new AtomicInteger();
        IllegalStateException expected = new IllegalStateException("object is null");

        IllegalStateException actual = assertThrows(IllegalStateException.class,
                () -> ObjUtil.get(null, value -> {
                    mapperCalls.incrementAndGet();
                    return String.valueOf(value);
                }, () -> {
                    supplierCalls.incrementAndGet();
                    return expected;
                }));

        assertSame(expected, actual);
        assertEquals(0, mapperCalls.get());
        assertEquals(1, supplierCalls.get());
    }

    /**
     * 验证 get 对非空对象不调用异常供应器
     */
    @Test
    void testGetWithValueAndThrowSupplier() {
        AtomicInteger supplierCalls = new AtomicInteger();

        String result = ObjUtil.get("taol", Object::toString, () -> {
            supplierCalls.incrementAndGet();
            return new IllegalStateException();
        });

        assertEquals("taol", result);
        assertEquals(0, supplierCalls.get());
    }

    /**
     * 验证 get 会拒绝空映射函数
     */
    @Test
    void testGetWithNullMapper() {
        assertThrows(NullPointerException.class, () -> ObjUtil.get("taol", null));
        assertThrows(NullPointerException.class, () -> ObjUtil.get(null, null));
    }

    /**
     * 验证对象存在时执行消费逻辑
     */
    @Test
    void testRunWithValue() {
        AtomicInteger calls = new AtomicInteger();

        ObjUtil.run("taol", value -> {
            assertEquals("taol", value);
            calls.incrementAndGet();
        });

        assertEquals(1, calls.get());
    }

    /**
     * 验证对象为空时不执行消费逻辑
     */
    @Test
    void testRunWithNullValue() {
        AtomicInteger calls = new AtomicInteger();

        ObjUtil.run(null, value -> calls.incrementAndGet());

        assertEquals(0, calls.get());
    }

    /**
     * 验证 run 对空对象使用异常供应器
     */
    @Test
    void testRunWithNullValueAndThrowSupplier() {
        AtomicInteger consumerCalls = new AtomicInteger();
        AtomicInteger supplierCalls = new AtomicInteger();
        IllegalArgumentException expected = new IllegalArgumentException("object is null");

        IllegalArgumentException actual = assertThrows(IllegalArgumentException.class,
                () -> ObjUtil.run(null, value -> consumerCalls.incrementAndGet(), () -> {
                    supplierCalls.incrementAndGet();
                    return expected;
                }));

        assertSame(expected, actual);
        assertEquals(0, consumerCalls.get());
        assertEquals(1, supplierCalls.get());
    }

    /**
     * 验证 run 对非空对象不调用异常供应器
     */
    @Test
    void testRunWithValueAndThrowSupplier() {
        AtomicInteger consumerCalls = new AtomicInteger();
        AtomicInteger supplierCalls = new AtomicInteger();

        ObjUtil.run("taol", value -> consumerCalls.incrementAndGet(), () -> {
            supplierCalls.incrementAndGet();
            return new IllegalStateException();
        });

        assertEquals(1, consumerCalls.get());
        assertEquals(0, supplierCalls.get());
    }

    /**
     * 验证 run 会拒绝空消费函数
     */
    @Test
    void testRunWithNullConsumer() {
        assertThrows(NullPointerException.class, () -> ObjUtil.run("taol", null));
        assertThrows(NullPointerException.class, () -> ObjUtil.run(null, null));
    }
}
