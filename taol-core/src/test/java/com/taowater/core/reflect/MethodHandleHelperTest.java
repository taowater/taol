package com.taowater.core.reflect;

import com.taowater.taol.core.convert.GetSetHelper;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MethodHandleHelperTest {

    @Test
    void accessPackagePrivateBeanMethod() throws NoSuchMethodException {
        PackagePrivateBean bean = new PackagePrivateBean();
        bean.setAge(42);

        Method getAge = PackagePrivateBean.class.getMethod("getAge");
        ToIntFunction<PackagePrivateBean> getter = (ToIntFunction<PackagePrivateBean>) GetSetHelper.buildGetterAccessor(
                PackagePrivateBean.class, getAge);
        assertNotNull(getter);
        assertEquals(42, getter.applyAsInt(bean));
    }

    @Getter
    @Setter
    static class PackagePrivateBean {
        private int age;
    }
}
