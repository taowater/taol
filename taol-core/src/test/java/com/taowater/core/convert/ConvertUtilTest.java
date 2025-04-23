package com.taowater.core.convert;

import com.taowater.taol.core.convert.ConvertUtil;
import lombok.Data;
import org.junit.jupiter.api.Test;

class ConvertUtilTest {

    @Data
    public static class Bean1 {
        private Long id;
        private String name;
        private Integer age;
    }

    @Data
    public static class Bean2 {
        private Long id;
        private String name;
        private Long age;
    }

    @Test
    void testCopy() {

        Bean1 bean1 = new Bean1();
        bean1.setId(1L);
        bean1.setName("<UNK>");
        bean1.setAge(1);
        Bean2 bean2 = new Bean2();

        ConvertUtil.copy(bean1, bean2);
        System.out.println(bean2);
    }


}