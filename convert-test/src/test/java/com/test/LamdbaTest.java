package com.test;

import com.taowater.taol.core.reflect.ClassUtil;
import com.test.bean.Bean1;
import org.junit.jupiter.api.Test;

class LambdaTest {

    @Test
    public void test() {

        Bean1 bean1 = ClassUtil.newInstance(Bean1.class);

        System.out.println(123);

    }


}
