package com.taowater.core.util;

import com.taowater.taol.core.util.EmptyUtil;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.taowater.core.TestUtil.initList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmptyUtilTest {

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
    }
}