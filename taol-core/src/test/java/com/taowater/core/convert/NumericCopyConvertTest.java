package com.taowater.core.convert;

import com.taowater.taol.core.convert.ConvertUtil;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 数值类型拓宽拷贝：标量字段与集合元素的排列组合转换。
 */
class NumericCopyConvertTest {

    private static final byte BYTE_VAL = 10;
    private static final short SHORT_VAL = 100;
    private static final int INT_VAL = 1000;
    private static final long LONG_VAL = 10_000L;

    @Test
    void convertScalarByteWidening() {
        ByteWidenSource source = new ByteWidenSource();
        source.setByteToShort(BYTE_VAL);
        source.setByteToInt(BYTE_VAL);
        source.setByteToLong(BYTE_VAL);
        source.setByteToFloat(BYTE_VAL);
        source.setByteToDouble(BYTE_VAL);
        source.setByteToShortWrapper(BYTE_VAL);
        source.setByteToIntWrapper(BYTE_VAL);
        source.setByteToLongWrapper(BYTE_VAL);
        source.setByteToFloatWrapper(BYTE_VAL);
        source.setByteToDoubleWrapper(BYTE_VAL);

        ByteWidenTarget target = ConvertUtil.convert(source, ByteWidenTarget.class);

        assertEquals((short) BYTE_VAL, target.getByteToShort());
        assertEquals((int) BYTE_VAL, target.getByteToInt());
        assertEquals((long) BYTE_VAL, target.getByteToLong());
        assertEquals((float) BYTE_VAL, target.getByteToFloat(), 0.001f);
        assertEquals((double) BYTE_VAL, target.getByteToDouble(), 0.001d);
        assertEquals(Short.valueOf(BYTE_VAL), target.getByteToShortWrapper());
        assertEquals(Integer.valueOf(BYTE_VAL), target.getByteToIntWrapper());
        assertEquals(Long.valueOf(BYTE_VAL), target.getByteToLongWrapper());
        assertEquals(Float.valueOf(BYTE_VAL), target.getByteToFloatWrapper());
        assertEquals(Double.valueOf(BYTE_VAL), target.getByteToDoubleWrapper());
    }

    @Test
    void convertScalarShortWidening() {
        ShortWidenSource source = new ShortWidenSource();
        source.setShortToInt(SHORT_VAL);
        source.setShortToLong(SHORT_VAL);
        source.setShortToFloat(SHORT_VAL);
        source.setShortToDouble(SHORT_VAL);
        source.setShortToIntWrapper(SHORT_VAL);
        source.setShortToLongWrapper(SHORT_VAL);
        source.setShortToFloatWrapper(SHORT_VAL);
        source.setShortToDoubleWrapper(SHORT_VAL);

        ShortWidenTarget target = ConvertUtil.convert(source, ShortWidenTarget.class);

        assertEquals((int) SHORT_VAL, target.getShortToInt());
        assertEquals((long) SHORT_VAL, target.getShortToLong());
        assertEquals((float) SHORT_VAL, target.getShortToFloat(), 0.001f);
        assertEquals((double) SHORT_VAL, target.getShortToDouble(), 0.001d);
        assertEquals(Integer.valueOf(SHORT_VAL), target.getShortToIntWrapper());
        assertEquals(Long.valueOf(SHORT_VAL), target.getShortToLongWrapper());
        assertEquals(Float.valueOf(SHORT_VAL), target.getShortToFloatWrapper());
        assertEquals(Double.valueOf(SHORT_VAL), target.getShortToDoubleWrapper());
    }

    @Test
    void convertScalarIntAndLongWidening() {
        IntLongWidenSource source = new IntLongWidenSource();
        source.setIntToLong(INT_VAL);
        source.setIntToFloat(INT_VAL);
        source.setIntToDouble(INT_VAL);
        source.setIntToLongWrapper(INT_VAL);
        source.setLongToFloat(LONG_VAL);
        source.setLongToDouble(LONG_VAL);
        source.setLongToDoubleWrapper(LONG_VAL);

        IntLongWidenTarget target = ConvertUtil.convert(source, IntLongWidenTarget.class);

        assertEquals((long) INT_VAL, target.getIntToLong());
        assertEquals((float) INT_VAL, target.getIntToFloat(), 0.001f);
        assertEquals((double) INT_VAL, target.getIntToDouble(), 0.001d);
        assertEquals(Long.valueOf(INT_VAL), target.getIntToLongWrapper());
        assertEquals((float) LONG_VAL, target.getLongToFloat(), 0.001f);
        assertEquals((double) LONG_VAL, target.getLongToDouble(), 0.001d);
        assertEquals(Double.valueOf(LONG_VAL), target.getLongToDoubleWrapper());
    }

    @Test
    void convertListByteElementWidening() {
        List<Byte> bytes = Arrays.asList((byte) 1, (byte) 2, (byte) 3);
        ListByteWidenSource source = new ListByteWidenSource();
        source.setListByteToListShort(bytes);
        source.setListByteToListInt(bytes);
        source.setListByteToListLong(bytes);
        source.setListByteToListFloat(bytes);

        ListByteWidenTarget target = ConvertUtil.convert(source, ListByteWidenTarget.class);

        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), target.getListByteToListShort());
        assertEquals(Arrays.asList(1, 2, 3), target.getListByteToListInt());
        assertEquals(Arrays.asList(1L, 2L, 3L), target.getListByteToListLong());
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), target.getListByteToListFloat());
    }

    @Test
    void convertListShortElementWidening() {
        List<Short> shorts = Arrays.asList((short) 10, (short) 20, (short) 30);
        ListShortWidenSource source = new ListShortWidenSource();
        source.setListShortToListInt(shorts);
        source.setListShortToListLong(shorts);
        source.setListShortToListFloat(shorts);
        source.setListShortToListDouble(shorts);

        ListShortWidenTarget target = ConvertUtil.convert(source, ListShortWidenTarget.class);

        assertEquals(Arrays.asList(10, 20, 30), target.getListShortToListInt());
        assertEquals(Arrays.asList(10L, 20L, 30L), target.getListShortToListLong());
        assertEquals(Arrays.asList(10.0f, 20.0f, 30.0f), target.getListShortToListFloat());
        assertEquals(Arrays.asList(10.0d, 20.0d, 30.0d), target.getListShortToListDouble());
    }

    @Test
    void convertListIntElementWidening() {
        List<Integer> ints = Arrays.asList(100, 200, 300);
        ListIntWidenSource source = new ListIntWidenSource();
        source.setListIntToListLong(ints);
        source.setListIntToListFloat(ints);
        source.setListIntToListDouble(ints);

        ListIntWidenTarget target = ConvertUtil.convert(source, ListIntWidenTarget.class);

        assertEquals(Arrays.asList(100L, 200L, 300L), target.getListIntToListLong());
        assertEquals(Arrays.asList(100.0f, 200.0f, 300.0f), target.getListIntToListFloat());
        assertEquals(Arrays.asList(100.0d, 200.0d, 300.0d), target.getListIntToListDouble());
    }

    @Test
    void convertSetToListElementWidening() {
        Set<Byte> byteSet = new LinkedHashSet<>(Arrays.asList((byte) 1, (byte) 2));
        Set<Short> shortSet = new LinkedHashSet<>(Arrays.asList((short) 10, (short) 20));
        SetCollectionWidenSource source = new SetCollectionWidenSource();
        source.setSetByteToListShort(byteSet);
        source.setSetByteToListLong(byteSet);
        source.setSetShortToListInt(shortSet);
        source.setSetShortToListLong(shortSet);
        source.setSetShortToListFloat(shortSet);

        SetCollectionWidenTarget target = ConvertUtil.convert(source, SetCollectionWidenTarget.class);

        assertEquals(Arrays.asList((short) 1, (short) 2), target.getSetByteToListShort());
        assertEquals(Arrays.asList(1L, 2L), target.getSetByteToListLong());
        assertEquals(Arrays.asList(10, 20), target.getSetShortToListInt());
        assertEquals(Arrays.asList(10L, 20L), target.getSetShortToListLong());
        assertEquals(Arrays.asList(10.0f, 20.0f), target.getSetShortToListFloat());
    }

    @Test
    void convertMixedCollectionCrossType() {
        MixedCollectionSource source = new MixedCollectionSource();
        source.setListLongToListInt(Arrays.asList(1L, 2L, 3L));
        source.setListIntToListLong(Arrays.asList(4, 5, 6));
        source.setSetShortToListLong(new LinkedHashSet<>(Arrays.asList((short) 7, (short) 8)));

        MixedCollectionTarget target = ConvertUtil.convert(source, MixedCollectionTarget.class);

        assertEquals(Arrays.asList(1, 2, 3), target.getListLongToListInt());
        assertEquals(Arrays.asList(4L, 5L, 6L), target.getListIntToListLong());
        assertEquals(Arrays.asList(7L, 8L), target.getSetShortToListLong());
    }

    // --- byte 标量拓宽 ---

    @Getter
    @Setter
    static class ByteWidenSource {
        private byte byteToShort;
        private byte byteToInt;
        private byte byteToLong;
        private byte byteToFloat;
        private byte byteToDouble;
        private Byte byteToShortWrapper;
        private Byte byteToIntWrapper;
        private Byte byteToLongWrapper;
        private Byte byteToFloatWrapper;
        private Byte byteToDoubleWrapper;
    }

    @Getter
    @Setter
    static class ByteWidenTarget {
        private short byteToShort;
        private int byteToInt;
        private long byteToLong;
        private float byteToFloat;
        private double byteToDouble;
        private Short byteToShortWrapper;
        private Integer byteToIntWrapper;
        private Long byteToLongWrapper;
        private Float byteToFloatWrapper;
        private Double byteToDoubleWrapper;
    }

    // --- short 标量拓宽 ---

    @Getter
    @Setter
    static class ShortWidenSource {
        private short shortToInt;
        private short shortToLong;
        private short shortToFloat;
        private short shortToDouble;
        private Short shortToIntWrapper;
        private Short shortToLongWrapper;
        private Short shortToFloatWrapper;
        private Short shortToDoubleWrapper;
    }

    @Getter
    @Setter
    static class ShortWidenTarget {
        private int shortToInt;
        private long shortToLong;
        private float shortToFloat;
        private double shortToDouble;
        private Integer shortToIntWrapper;
        private Long shortToLongWrapper;
        private Float shortToFloatWrapper;
        private Double shortToDoubleWrapper;
    }

    // --- int / long 标量拓宽 ---

    @Getter
    @Setter
    static class IntLongWidenSource {
        private int intToLong;
        private int intToFloat;
        private int intToDouble;
        private Integer intToLongWrapper;
        private long longToFloat;
        private long longToDouble;
        private Long longToDoubleWrapper;
    }

    @Getter
    @Setter
    static class IntLongWidenTarget {
        private long intToLong;
        private float intToFloat;
        private double intToDouble;
        private Long intToLongWrapper;
        private float longToFloat;
        private double longToDouble;
        private Double longToDoubleWrapper;
    }

    // --- List<Byte> 元素拓宽 ---

    @Getter
    @Setter
    static class ListByteWidenSource {
        private List<Byte> listByteToListShort;
        private List<Byte> listByteToListInt;
        private List<Byte> listByteToListLong;
        private List<Byte> listByteToListFloat;
    }

    @Getter
    @Setter
    static class ListByteWidenTarget {
        private List<Short> listByteToListShort;
        private List<Integer> listByteToListInt;
        private List<Long> listByteToListLong;
        private List<Float> listByteToListFloat;
    }

    // --- List<Short> 元素拓宽 ---

    @Getter
    @Setter
    static class ListShortWidenSource {
        private List<Short> listShortToListInt;
        private List<Short> listShortToListLong;
        private List<Short> listShortToListFloat;
        private List<Short> listShortToListDouble;
    }

    @Getter
    @Setter
    static class ListShortWidenTarget {
        private List<Integer> listShortToListInt;
        private List<Long> listShortToListLong;
        private List<Float> listShortToListFloat;
        private List<Double> listShortToListDouble;
    }

    // --- List<Integer> 元素拓宽 ---

    @Getter
    @Setter
    static class ListIntWidenSource {
        private List<Integer> listIntToListLong;
        private List<Integer> listIntToListFloat;
        private List<Integer> listIntToListDouble;
    }

    @Getter
    @Setter
    static class ListIntWidenTarget {
        private List<Long> listIntToListLong;
        private List<Float> listIntToListFloat;
        private List<Double> listIntToListDouble;
    }

    // --- Set 转 List 元素拓宽 ---

    @Getter
    @Setter
    static class SetCollectionWidenSource {
        private Set<Byte> setByteToListShort;
        private Set<Byte> setByteToListLong;
        private Set<Short> setShortToListInt;
        private Set<Short> setShortToListLong;
        private Set<Short> setShortToListFloat;
    }

    @Getter
    @Setter
    static class SetCollectionWidenTarget {
        private List<Short> setByteToListShort;
        private List<Long> setByteToListLong;
        private List<Integer> setShortToListInt;
        private List<Long> setShortToListLong;
        private List<Float> setShortToListFloat;
    }

    // --- 混合集合交叉转换 ---

    @Getter
    @Setter
    static class MixedCollectionSource {
        private List<Long> listLongToListInt;
        private List<Integer> listIntToListLong;
        private Set<Short> setShortToListLong;
    }

    @Getter
    @Setter
    static class MixedCollectionTarget {
        private List<Integer> listLongToListInt;
        private List<Long> listIntToListLong;
        private List<Long> setShortToListLong;
    }
}
