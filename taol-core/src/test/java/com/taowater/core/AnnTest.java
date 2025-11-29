package com.taowater.core;

import com.taowater.taol.core.ann.AliasFor;
import com.taowater.taol.core.ann.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnnTest {

    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface Ann {
        @AliasFor(annotation = Ann.class, value = "attr2")
        String value() default "";

        String attr1() default "";

        String attr2() default "";
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Ann(attr1 = "fromSubAnn")
    public @interface SubAnn {
        @AliasFor(annotation = Ann.class, value = "attr2")
        String value() default "";

        @AliasFor(annotation = Ann.class, value = "attr2")
        String attr2() default "";
    }


    @SubAnn(value = "attr2", attr2 = "attr2")
    public static class ClassAnn {

    }

    @Test
    void test() {

        Ann ann = AnnotationUtil.getAnnotation(ClassAnn.class, Ann.class);

        assert ann != null;

        assertEquals("fromSubAnn", ann.attr1());
        assertEquals("attr2", ann.attr2());
        assertEquals("attr2", ann.value());
    }
}
