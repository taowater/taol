package com.taowater.core.convert;

import com.taowater.taol.core.convert.BeanMetadata;
import com.taowater.taol.core.convert.ConvertUtil;
import com.taowater.taol.core.convert.CopyException;
import com.taowater.taol.core.convert.GetSetHelper;
import com.taowater.taol.core.function.*;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.*;

import static org.junit.jupiter.api.Assertions.*;

class GetSetHelperTest {

    @Test
    void buildGetterAccessorUsesResolvedMethod() {
        ToIntFunction<PrimitiveBean> getter = (ToIntFunction<PrimitiveBean>) GetSetHelper.buildGetterAccessor(
                PrimitiveBean.class, BeanMetadata.of(PrimitiveBean.class).getField("age").getGetterMethod());
        assertNotNull(getter);
        PrimitiveBean bean = new PrimitiveBean();
        bean.setAge(18);
        assertEquals(18, getter.applyAsInt(bean));
    }

    @Test
    void buildSetterAccessorUsesPrimitiveConsumer() {
        ObjIntConsumer<PrimitiveBean> setter = (ObjIntConsumer<PrimitiveBean>) GetSetHelper.buildSetterAccessor(
                PrimitiveBean.class, BeanMetadata.of(PrimitiveBean.class).getField("age").getSetterMethod());
        assertNotNull(setter);
        PrimitiveBean bean = new PrimitiveBean();
        setter.accept(bean, 20);
        assertEquals(20, bean.getAge());
    }

    @Test
    void resolveBooleanGetterForPrimitiveField() {
        BooleanBean bean = new BooleanBean();
        bean.setFlag(true);
        ToLongFunction<BooleanBean> idGetter = (ToLongFunction<BooleanBean>) GetSetHelper.buildGetterAccessor(
                BooleanBean.class, BeanMetadata.of(BooleanBean.class).getField("id").getGetterMethod());
        assertEquals(1L, idGetter.applyAsLong(bean));

        ToBooleanFunction<BooleanBean> flagGetter = (ToBooleanFunction<BooleanBean>) GetSetHelper.buildGetterAccessor(
                BooleanBean.class, BeanMetadata.of(BooleanBean.class).getField("flag").getGetterMethod());
        assertTrue(flagGetter.applyAsBoolean(bean));
        assertEquals("isFlag", BeanMetadata.of(BooleanBean.class).getField("flag").getGetterMethod().getName());

        ObjBooleanConsumer<PrimitiveBean> flagSetter = (ObjBooleanConsumer<PrimitiveBean>) GetSetHelper.buildSetterAccessor(
                PrimitiveBean.class, BeanMetadata.of(PrimitiveBean.class).getField("flag").getSetterMethod());
        PrimitiveBean flagBean = new PrimitiveBean();
        flagSetter.accept(flagBean, true);
        assertTrue(flagBean.isFlag());
    }

    @Test
    void legacyBuildGetterAndSetterStillWork() {
        StringBean bean = new StringBean();
        Function<StringBean, String> getter = GetSetHelper.buildGetter(StringBean.class, "name");
        BiConsumer<StringBean, String> setter = GetSetHelper.buildSetter(StringBean.class, "name");
        setter.accept(bean, "taol");
        assertEquals("taol", getter.apply(bean));
    }

    @Test
    void convertCopiesPrimitiveFieldsWithoutNullSkip() {
        PrimitiveSource source = new PrimitiveSource();
        source.setAge(0);
        source.setId(9L);
        PrimitiveTarget target = ConvertUtil.convert(source, PrimitiveTarget.class);
        assertEquals(0, target.getAge());
        assertEquals(9L, target.getId());
    }

    @Test
    void buildAccessorForAllPrimitiveTypes() {
        AllPrimitiveBean bean = new AllPrimitiveBean();
        bean.setFlag(true);
        bean.setByteValue((byte) 1);
        bean.setShortValue((short) 2);
        bean.setCharValue('c');
        bean.setIntValue(3);
        bean.setLongValue(4L);
        bean.setFloatValue(5.5f);
        bean.setDoubleValue(6.6d);

        BeanMetadata metadata = BeanMetadata.of(AllPrimitiveBean.class);

        assertTrue(((ToBooleanFunction<AllPrimitiveBean>) GetSetHelper.buildGetterAccessor(
                AllPrimitiveBean.class, metadata.getField("flag").getGetterMethod())).applyAsBoolean(bean));
        assertEquals((byte) 1, ((ToByteFunction<AllPrimitiveBean>) GetSetHelper.buildGetterAccessor(
                AllPrimitiveBean.class, metadata.getField("byteValue").getGetterMethod())).applyAsByte(bean));
        assertEquals((short) 2, ((ToShortFunction<AllPrimitiveBean>) GetSetHelper.buildGetterAccessor(
                AllPrimitiveBean.class, metadata.getField("shortValue").getGetterMethod())).applyAsShort(bean));
        assertEquals('c', ((ToCharFunction<AllPrimitiveBean>) GetSetHelper.buildGetterAccessor(
                AllPrimitiveBean.class, metadata.getField("charValue").getGetterMethod())).applyAsChar(bean));
        assertEquals(3, ((ToIntFunction<AllPrimitiveBean>) GetSetHelper.buildGetterAccessor(
                AllPrimitiveBean.class, metadata.getField("intValue").getGetterMethod())).applyAsInt(bean));
        assertEquals(4L, ((ToLongFunction<AllPrimitiveBean>) GetSetHelper.buildGetterAccessor(
                AllPrimitiveBean.class, metadata.getField("longValue").getGetterMethod())).applyAsLong(bean));
        assertEquals(5.5f, ((ToFloatFunction<AllPrimitiveBean>) GetSetHelper.buildGetterAccessor(
                AllPrimitiveBean.class, metadata.getField("floatValue").getGetterMethod())).applyAsFloat(bean));
        assertEquals(6.6d, ((ToDoubleFunction<AllPrimitiveBean>) GetSetHelper.buildGetterAccessor(
                AllPrimitiveBean.class, metadata.getField("doubleValue").getGetterMethod())).applyAsDouble(bean));

        AllPrimitiveBean target = new AllPrimitiveBean();
        ((ObjBooleanConsumer<AllPrimitiveBean>) GetSetHelper.buildSetterAccessor(
                AllPrimitiveBean.class, metadata.getField("flag").getSetterMethod())).accept(target, false);
        ((ObjByteConsumer<AllPrimitiveBean>) GetSetHelper.buildSetterAccessor(
                AllPrimitiveBean.class, metadata.getField("byteValue").getSetterMethod())).accept(target, (byte) 9);
        ((ObjFloatConsumer<AllPrimitiveBean>) GetSetHelper.buildSetterAccessor(
                AllPrimitiveBean.class, metadata.getField("floatValue").getSetterMethod())).accept(target, 9.9f);

        assertFalse(target.isFlag());
        assertEquals((byte) 9, target.getByteValue());
        assertEquals(9.9f, target.getFloatValue());
    }

    @Getter
    @Setter
    static class AllPrimitiveBean {
        private boolean flag;
        private byte byteValue;
        private short shortValue;
        private char charValue;
        private int intValue;
        private long longValue;
        private float floatValue;
        private double doubleValue;
    }

    @Getter
    @Setter
    static class PrimitiveBean {
        private int age;
        private boolean flag;
    }

    @Getter
    @Setter
    static class BooleanBean {
        private long id = 1L;
        private boolean flag;
    }

    @Getter
    @Setter
    static class StringBean {
        private String name;
    }

    @Getter
    @Setter
    static class PrimitiveSource {
        private int age;
        private long id;
    }

    @Test
    void convertCreatesPackagePrivateTarget() {
        PrimitiveSource source = new PrimitiveSource();
        source.setAge(7);
        PrimitiveTarget target = ConvertUtil.convert(source, PrimitiveTarget.class);
        assertEquals(7, target.getAge());
    }

    @Test
    void convertCopiesNumericValuesByTargetType() {
        NumericSource source = new NumericSource();
        source.setId(1);
        source.setAge(2L);
        source.setCount(3);

        NumericTarget target = ConvertUtil.convert(source, NumericTarget.class);

        assertEquals(Long.valueOf(1L), target.getId());
        assertEquals(Integer.valueOf(2), target.getAge());
        assertEquals(Long.valueOf(3L), target.getCount());
    }

    @Test
    void convertThrowsWhenNumericValueOverflowsTargetType() {
        OverflowSource source = new OverflowSource();
        source.setA(2147483648L);

        assertThrows(CopyException.class, () -> ConvertUtil.convert(source, IntTarget.class));
    }

    @Test
    void convertCopiesListElementsByTargetGenericType() {
        ListSource source = new ListSource();
        source.setIds(Arrays.asList(1L, 2L, 3L));
        source.setCodes(Arrays.asList(4, 5, 6));

        ListTarget target = ConvertUtil.convert(source, ListTarget.class);

        assertEquals(Arrays.asList(1, 2, 3), target.getIds());
        assertEquals(Integer.class, target.getIds().get(0).getClass());
        assertEquals(Arrays.asList(4L, 5L, 6L), target.getCodes());
        assertEquals(Long.class, target.getCodes().get(0).getClass());
    }

    @Test
    void convertThrowsWhenListElementOverflowsTargetGenericType() {
        ListSource source = new ListSource();
        source.setIds(Arrays.asList(2147483648L));

        assertThrows(CopyException.class, () -> ConvertUtil.convert(source, ListTarget.class));
    }

    @Getter
    @Setter
    static class PrimitiveTarget {
        private int age;
        private Long id;
    }

    @Getter
    @Setter
    static class NumericSource {
        private Integer id;
        private Long age;
        private int count;
    }

    @Getter
    @Setter
    static class NumericTarget {
        private Long id;
        private Integer age;
        private Long count;
    }

    @Getter
    @Setter
    static class OverflowSource {
        private Long a;
    }

    @Getter
    @Setter
    static class IntTarget {
        private int a;
    }

    @Getter
    @Setter
    static class ListSource {
        private List<Long> ids;
        private List<Integer> codes;
    }

    @Getter
    @Setter
    static class ListTarget {
        private List<Integer> ids;
        private List<Long> codes;
    }
}
