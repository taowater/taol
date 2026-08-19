package com.taowater.core.reflect;

import com.taowater.taol.core.reflect.ReflectUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReflectUtilTest {

    /**
     * 验证字段查找支持当前类和父类
     */
    @Test
    void testGetField() {
        Field childField = ReflectUtil.getField(ChildBean.class, "child");
        Field parentField = ReflectUtil.getField(ChildBean.class, "parent");

        assertEquals(int.class, childField.getType());
        assertEquals(String.class, parentField.getType());
        assertEquals(int.class, ReflectUtil.getFieldType(ChildBean.class, "child"));
    }

    /**
     * 验证字段不存在时抛出带字段信息的异常
     */
    @Test
    void testGetFieldMissing() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> ReflectUtil.getField(ChildBean.class, "missing"));

        assertEquals("The field \"missing\" does not exist in Class " + ChildBean.class.getName(),
                exception.getMessage());
    }

    static class ParentBean {
        private String parent;
    }

    static class ChildBean extends ParentBean {
        private int child;
    }
}
