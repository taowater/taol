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

    /**
     * 覆盖多参数中存在空值、非空值及两者并存的判断
     */
    @Test
    void testHadEmpty() {
        assertTrue(EmptyUtil.isHadEmpty((Object[]) null));
        assertFalse(EmptyUtil.isHadEmpty());
        assertTrue(EmptyUtil.isHadEmpty("taol", ""));
        assertFalse(EmptyUtil.isHadEmpty("taol", 1));

        assertFalse(EmptyUtil.isHadNotEmpty((Object[]) null));
        assertFalse(EmptyUtil.isHadNotEmpty());
        assertFalse(EmptyUtil.isHadNotEmpty("", null, initList()));
        assertTrue(EmptyUtil.isHadNotEmpty("", "taol"));

        assertFalse(EmptyUtil.isHadBoth((Object[]) null));
        assertFalse(EmptyUtil.isHadBoth());
        assertFalse(EmptyUtil.isHadBoth("", null));
        assertFalse(EmptyUtil.isHadBoth("taol", 1));
        assertTrue(EmptyUtil.isHadBoth("", "taol"));
        assertTrue(EmptyUtil.isHadBoth("taol", ""));
    }

    /**
     * 覆盖多参数中存在 null 和非 null 的判断
     */
    @Test
    void testHadNull() {
        assertTrue(EmptyUtil.isHadNull((Object[]) null));
        assertFalse(EmptyUtil.isHadNull());
        assertTrue(EmptyUtil.isHadNull("taol", null));
        assertFalse(EmptyUtil.isHadNull("taol", 1));

        assertFalse(EmptyUtil.isHadNotNull((Object[]) null));
        assertFalse(EmptyUtil.isHadNotNull());
        assertFalse(EmptyUtil.isHadNotNull(null, null));
        assertTrue(EmptyUtil.isHadNotNull(null, "taol"));
    }

    /**
     * 覆盖多参数全部为空或全部非空的判断
     */
    @Test
    void testAllEmpty() {
        assertTrue(EmptyUtil.isAllEmpty((Object[]) null));
        assertTrue(EmptyUtil.isAllEmpty());
        assertTrue(EmptyUtil.isAllEmpty("", null, initList()));
        assertFalse(EmptyUtil.isAllEmpty("", "taol"));

        assertFalse(EmptyUtil.isAllNotEmpty((Object[]) null));
        assertTrue(EmptyUtil.isAllNotEmpty());
        assertTrue(EmptyUtil.isAllNotEmpty("taol", 1));
        assertFalse(EmptyUtil.isAllNotEmpty("taol", ""));
    }

    /**
     * 覆盖空白判断的各个短路分支及非字符串实现
     */
    @Test
    void testBlank() {
        assertTrue(EmptyUtil.isBlank(null));
        assertTrue(EmptyUtil.isBlank(""));
        assertTrue(EmptyUtil.isBlank(" \t\r\n"));
        assertTrue(EmptyUtil.isBlank(new StringBuilder("  ")));
        assertFalse(EmptyUtil.isBlank(" taol "));
        assertFalse(EmptyUtil.isBlank(new StringBuilder("taol")));
    }

    /**
     * 验证非空白判断与空白判断结果相反
     */
    @Test
    void testNotBlank() {
        assertFalse(EmptyUtil.isNotBlank(null));
        assertFalse(EmptyUtil.isNotBlank(" \t"));
        assertTrue(EmptyUtil.isNotBlank("taol"));
    }
}
