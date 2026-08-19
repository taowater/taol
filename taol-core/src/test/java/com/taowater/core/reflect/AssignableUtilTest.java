package com.taowater.core.reflect;

import com.taowater.taol.core.reflect.AssignableUtil;
import com.taowater.taol.core.reflect.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignableUtilTest {

    /**
     * 验证基本类型、包装类型及继承关系的赋值判断
     */
    @Test
    void testClassAssignable() {
        assertTrue(AssignableUtil.isAssignable(int.class, Integer.class));
        assertTrue(AssignableUtil.isAssignable(Integer.class, int.class));
        assertTrue(AssignableUtil.isAssignable(String.class, Object.class));
        assertFalse(AssignableUtil.isAssignable(Object.class, String.class));
        assertFalse(AssignableUtil.isAssignable(null, String.class));
    }

    /**
     * 验证参数化类型的递归赋值判断
     */
    @Test
    void testParameterizedTypeAssignable() {
        Type integerList = new TypeReference<List<Integer>>() {
        }.getType();
        Type numberList = new TypeReference<List<Number>>() {
        }.getType();
        Type stringList = new TypeReference<List<String>>() {
        }.getType();

        assertTrue(AssignableUtil.isAssignable(integerList, numberList));
        assertFalse(AssignableUtil.isAssignable(integerList, stringList));
    }
}
