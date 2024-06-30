package com.taowater.core.util;

import com.taowater.taol.core.util.EmptyUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.map.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmptyUtilTest {

    @Test
    void testEmpty() {

        assertTrue(EmptyUtil.isEmpty(null));
        assertFalse(EmptyUtil.isEmpty(1));
        assertFalse(EmptyUtil.isEmpty("1"));
        assertTrue(EmptyUtil.isEmpty(""));
        assertFalse(EmptyUtil.isEmpty(ListUtil.of(1, 2, 3)));
        assertTrue(EmptyUtil.isEmpty(ListUtil.of()));
        assertTrue(EmptyUtil.isEmpty(new HashMap<>()));
        assertFalse(EmptyUtil.isEmpty(MapUtil.builder(new HashMap<String, Object>()).put("123", "123")));
        assertFalse(EmptyUtil.isEmpty(new String[]{"123", "", ""}));
        assertFalse(EmptyUtil.isEmpty(new String[]{"", "", ""}));
        assertTrue(EmptyUtil.isEmpty(new String[]{}));
    }
}