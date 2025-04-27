package com.taowater.core.reflect;

import com.taowater.taol.core.reflect.GenericTypeUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassUtilTest {

    interface InterfaceA<T> {
        default Class<T> getAType() {
            return (Class<T>) GenericTypeUtil.getTypeArgument(this.getClass(), InterfaceA.class, 0);
        }
    }

    interface InterfaceB<T, Y> {
        default Class<T> getBType() {
            return (Class<T>) GenericTypeUtil.getTypeArgument(this.getClass(), InterfaceB.class, 0);
        }

        default Class<Y> getB2Type() {
            return (Class<Y>) GenericTypeUtil.getTypeArgument(this.getClass(), InterfaceB.class, 1);
        }
    }

    interface InterfaceC<T, T2> extends InterfaceA<T> {
    }

    interface InterfaceD<T, T2> extends InterfaceA<T>, InterfaceB<T, T2> {
    }

    static class ClassA implements InterfaceC<Integer, Boolean>, InterfaceB<String, Double> {

    }

    static class ClassB implements InterfaceB<String, Long>, InterfaceA<Integer> {

    }

    static class ClassD<T> extends ClassB implements InterfaceA<Integer> {

    }

    static class ClassC extends ClassD<BigDecimal> implements InterfaceB<String, Long>, InterfaceC<Integer, BigDecimal> {
    }

    static class ClassE implements InterfaceD<String, Long> {
    }

    @Test
    void testInterfaceGenericType() {
        ClassA a = new ClassA();
        ClassB b = new ClassB();
        ClassC c = new ClassC();

        assertEquals(c.getAType(), Integer.class);
        assertEquals(a.getBType(), String.class);
        assertEquals(a.getB2Type(), Double.class);
        assertEquals(b.getAType(), Integer.class);
        assertEquals(b.getBType(), String.class);
        assertEquals(b.getB2Type(), Long.class);
    }
}