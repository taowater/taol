package com.taowater.core.convert;

import com.taowater.taol.core.convert.ConvertUtil;
import com.taowater.taol.core.util.CollUtil;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class ConvertUtilTest {

    @Data
    public static class Bean1 {
        private Long id;
        private String name;
        private AtomicInteger age;
        private List<Integer> list;
        private List<Integer>[] list2;
    }

    @Data
    public static class Bean2 {
        private Long id;
        private String name;
        private Number age;
        private List<Long> list;
        private List<Number>[] list2;
    }

    @Test
    void testCopy() {

        Bean1 bean1 = new Bean1();
        bean1.setId(1L);
        bean1.setName("jack");
        bean1.setAge(new AtomicInteger(123));
        bean1.setList(CollUtil.list(1, 4, 6, 9));
        bean1.setList2(new List[]{CollUtil.list(1), CollUtil.list(5), CollUtil.list(5)});
        Bean2 bean2 = new Bean2();

        ConvertUtil.copy(bean1, bean2);
        System.out.println(bean2);
        Bean2 bean3 = ConvertUtil.convert(bean2, Bean2.class);
        System.out.println(bean3);
    }


}