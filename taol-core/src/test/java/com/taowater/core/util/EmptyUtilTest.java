package com.taowater.core.util;

import com.taowater.taol.core.inter.Emptyable;
import com.taowater.taol.core.util.EmptyUtil;
import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.taowater.core.TestUtil.initList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmptyUtilTest {

    @Data
    @Builder
    static class TestDemo implements Emptyable {
        private String name;
        private int age;

        @Override
        public boolean isEmpty() {
            return age > 3;
        }
    }

    @Test
    void testEmpty() {
        Map<String, Object> map = new HashMap<>();
        map.put("123", "123");
        assertTrue(EmptyUtil.isEmpty(null));
        assertFalse(EmptyUtil.isEmpty(1));
        assertFalse(EmptyUtil.isEmpty("1"));
        assertTrue(EmptyUtil.isEmpty(""));
        assertFalse(EmptyUtil.isEmpty(initList(1, 2, 3)));
        assertTrue(EmptyUtil.isEmpty(initList()));
        assertTrue(EmptyUtil.isEmpty(new HashMap<>()));
        assertFalse(EmptyUtil.isEmpty(map));
        assertFalse(EmptyUtil.isEmpty(new String[]{"123", "", ""}));
        assertFalse(EmptyUtil.isEmpty(new String[]{"", "", ""}));
        assertTrue(EmptyUtil.isEmpty(new String[]{}));
        assertFalse(EmptyUtil.isEmpty(TestDemo.builder().name("123").build()));
        assertTrue(EmptyUtil.isEmpty(TestDemo.builder().age(4).build()));
    }
}