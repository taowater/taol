package com.taowater.core.convert;

import com.taowater.taol.core.convert.ConvertUtil;
import com.taowater.taol.core.util.CollUtil;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.List;

class ConvertUtilTest {

    @Data
    public static class Bean1 {
        private Long id;
        private String name;
        private Integer age;
        private List<Integer> list;
    }

    @Data
    public static class Bean2 {
        private Long id;
        private String name;
        private Long age;
        private List<String> list;
    }

    @Test
    void testCopy() {

        Bean1 bean1 = new Bean1();
        bean1.setId(1L);
        bean1.setName("jack");
        bean1.setAge(1);
        bean1.setList(CollUtil.list(1, 4, 6, 9));
        Bean2 bean2 = new Bean2();

        ConvertUtil.copy(bean1, bean2);
        System.out.println(bean2);
        Bean2 bean3 = ConvertUtil.convert(bean2, Bean2.class);
        System.out.println(bean3);
    }


}