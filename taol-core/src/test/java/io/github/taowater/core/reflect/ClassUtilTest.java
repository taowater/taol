package io.github.taowater.core.reflect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassUtilTest {

    interface InterfaceA<T> {
        default Class<T> getAType() {
            return ClassUtil.getInterfaceGenericType(this.getClass(), InterfaceA.class, 0);
        }
    }

    interface InterfaceB<T, Y> {
        default Class<T> getBType() {
            return ClassUtil.getInterfaceGenericType(this.getClass(), InterfaceB.class, 0);
        }

        default Class<Y> getB2Type() {
            return ClassUtil.getInterfaceGenericType(this.getClass(), InterfaceB.class, 1);
        }
    }

    static class ClassA implements InterfaceA<Integer>, InterfaceB<String, Double> {

    }

    static class ClassB implements InterfaceB<String, Long>, InterfaceA<Integer> {

    }

    @Test
    void testInterfaceGenericType() {
        ClassA a = new ClassA();
        ClassB b = new ClassB();
        assertEquals(a.getAType(), Integer.class);
        assertEquals(a.getBType(), String.class);
        assertEquals(a.getB2Type(), Double.class);
        assertEquals(b.getAType(), Integer.class);
        assertEquals(b.getBType(), String.class);
        assertEquals(b.getB2Type(), Long.class);
    }
}