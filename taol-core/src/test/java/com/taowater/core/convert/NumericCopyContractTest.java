package com.taowater.core.convert;

import com.taowater.taol.core.convert.ConvertUtil;
import com.taowater.taol.core.convert.CopyException;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 数值拷贝契约：JSON 可表达的数字类型在同名属性下应表现一致。
 * <ul>
 *   <li>精度放大（拓宽）：正常拷贝</li>
 *   <li>精度缩小（窄化）：范围内正常拷贝，否则 {@link CopyException}</li>
 *   <li>集合元素与标量遵循相同规则</li>
 * </ul>
 */
class NumericCopyContractTest {

    @Test
    void wideningIntegralScalarsToWiderTypes() {
        VByte s1 = new VByte();
        s1.setV((byte) 10);
        assertEquals(10L, convert(s1, VWLong.class).getV());

        VShort s2 = new VShort();
        s2.setV((short) 100);
        assertEquals(100L, convert(s2, VWLong.class).getV());

        VInt s3 = new VInt();
        s3.setV(1_000);
        assertEquals(1_000L, convert(s3, VWLong.class).getV());
        assertEquals(new BigInteger("1000"), convert(s3, VWBigInteger.class).getV());
        assertEquals(new BigDecimal("1000"), convert(s3, VWBigDecimal.class).getV());

        VInt s4 = new VInt();
        s4.setV(100);
        assertEquals(100.0d, convert(s4, VWDouble.class).getV(), 0.001d);
    }

    @Test
    void wideningFloatingScalarsToDouble() {
        VFloat s = new VFloat();
        s.setV(3.5f);
        assertEquals(3.5d, convert(s, VWDouble.class).getV(), 0.001d);
        assertEquals(new BigDecimal("3.5"), convert(s, VWBigDecimal.class).getV());
    }

    @Test
    void narrowingIntegralScalarsInRange() {
        VWLong s1 = new VWLong();
        s1.setV(100L);
        assertEquals(100, convert(s1, VInt.class).getV());

        VWInteger s2 = new VWInteger();
        s2.setV(100);
        assertEquals((short) 100, convert(s2, VShort.class).getV());

        VWShort s3 = new VWShort();
        s3.setV((short) 10);
        assertEquals((byte) 10, convert(s3, VByte.class).getV());

        VWBigInteger s4 = new VWBigInteger();
        s4.setV(BigInteger.valueOf(100));
        assertEquals(100, convert(s4, VInt.class).getV());

        VWBigDecimal s5 = new VWBigDecimal();
        s5.setV(new BigDecimal("100"));
        assertEquals(100L, convert(s5, VWLong.class).getV());
    }

    @Test
    void narrowingIntegralScalarsOverflowThrows() {
        VWLong s1 = new VWLong();
        s1.setV(2_147_483_648L);
        assertThrows(CopyException.class, () -> convert(s1, VInt.class));
        assertThrows(CopyException.class, () -> convert(s1, VWInteger.class));

        VWInteger s2 = new VWInteger();
        s2.setV(1_000);
        assertThrows(CopyException.class, () -> convert(s2, VByte.class));

        VWBigInteger s3 = new VWBigInteger();
        s3.setV(BigInteger.valueOf(1_000_000_000_000L));
        assertThrows(CopyException.class, () -> convert(s3, VInt.class));
    }

    @Test
    void fractionalNumberToIntegralThrows() {
        VWDouble s1 = new VWDouble();
        s1.setV(100.5d);
        assertThrows(CopyException.class, () -> convert(s1, VInt.class));

        VWFloat s2 = new VWFloat();
        s2.setV(1.5f);
        assertThrows(CopyException.class, () -> convert(s2, VWInteger.class));

        VWBigDecimal s3 = new VWBigDecimal();
        s3.setV(new BigDecimal("10.5"));
        assertThrows(CopyException.class, () -> convert(s3, VWLong.class));
    }

    @Test
    void integralFloatingScalarToIntegralInRange() {
        VWDouble s1 = new VWDouble();
        s1.setV(100.0d);
        assertEquals(100, convert(s1, VInt.class).getV());

        VWFloat s2 = new VWFloat();
        s2.setV(100.0f);
        assertEquals(100, convert(s2, VWInteger.class).getV());
    }

    @Test
    void narrowingDoubleToFloatInRange() {
        VWDouble s = new VWDouble();
        s.setV(3.5d);
        assertEquals(3.5f, convert(s, VFloat.class).getV(), 0.001f);
        assertEquals(3.5f, convert(s, VWFloat.class).getV(), 0.001f);
    }

    @Test
    void narrowingDoubleToFloatOverflowThrows() {
        VWDouble s = new VWDouble();
        s.setV(Double.MAX_VALUE);
        assertThrows(CopyException.class, () -> convert(s, VFloat.class));
    }

    @Test
    void longToDoubleConvertsWithBestAvailablePrecision() {
        VLong exact = new VLong();
        exact.setV(9_007_199_254_740_992L); // 2^53，double 可精确表示
        assertEquals(9_007_199_254_740_992.0d, convert(exact, VDouble.class).getV(), 0.0d);
        assertEquals(9_007_199_254_740_992.0d, convert(exact, VWDouble.class).getV(), 0.0d);

        VLong rounded = new VLong();
        rounded.setV(9_007_199_254_740_993L); // 2^53 + 1，转为最近可表示的 double
        assertEquals(9_007_199_254_740_992.0d, convert(rounded, VDouble.class).getV(), 0.0d);

        VLong max = new VLong();
        max.setV(Long.MAX_VALUE);
        assertEquals((double) Long.MAX_VALUE, convert(max, VDouble.class).getV(), 0.0d);

        VLong min = new VLong();
        min.setV(Long.MIN_VALUE);
        assertEquals((double) Long.MIN_VALUE, convert(min, VDouble.class).getV(), 0.0d);

        VWBigInteger huge = new VWBigInteger();
        huge.setV(new BigInteger("9223372036854775808")); // Long.MAX_VALUE + 1
        assertEquals(9223372036854775808.0d, convert(huge, VWDouble.class).getV(), 0.0d);
    }

    @Test
    void longCollectionToDoubleCollectionConvertsAllElements() {
        VLListLong source = new VLListLong();
        source.setV(Arrays.asList(1L, Long.MAX_VALUE, 9_007_199_254_740_993L));

        VLListDouble target = convert(source, VLListDouble.class);

        assertEquals(1.0d, target.getV().get(0), 0.0d);
        assertEquals((double) Long.MAX_VALUE, target.getV().get(1), 0.0d);
        assertEquals(9_007_199_254_740_992.0d, target.getV().get(2), 0.0d);
    }

    @Test
    void longArrayToDoubleArrayConvertsAllElements() {
        VArrayLong source = new VArrayLong();
        source.setV(new long[]{1L, Long.MAX_VALUE, 9_007_199_254_740_993L});

        VArrayDouble target = convert(source, VArrayDouble.class);

        assertEquals(1.0d, target.getV()[0], 0.0d);
        assertEquals((double) Long.MAX_VALUE, target.getV()[1], 0.0d);
        assertEquals(9_007_199_254_740_992.0d, target.getV()[2], 0.0d);
    }

    @Test
    void collectionElementsFollowScalarWideningRules() {
        VLListByte source = new VLListByte();
        source.setV(Arrays.asList((byte) 1, (byte) 2));
        assertEquals(Arrays.asList(1L, 2L), convert(source, VLListLong.class).getV());
        assertEquals(Arrays.asList(1.0f, 2.0f), convert(source, VLListFloat.class).getV());
    }

    @Test
    void collectionElementsFollowScalarNarrowingRules() {
        VLListLong inRange = new VLListLong();
        inRange.setV(Arrays.asList(1L, 2L, 3L));
        assertEquals(Arrays.asList(1, 2, 3), convert(inRange, VLListInteger.class).getV());

        VLListLong overflow = new VLListLong();
        overflow.setV(Arrays.asList(2_147_483_647L, 2_147_483_648L));
        assertThrows(CopyException.class, () -> convert(overflow, VLListInteger.class));
    }

    @Test
    void collectionElementsFollowFractionalToIntegralRules() {
        VLListDouble source = new VLListDouble();
        source.setV(Arrays.asList(1.0d, 2.5d));
        assertThrows(CopyException.class, () -> convert(source, VLListInteger.class));
    }

    @Test
    void primitiveArrayToListUsesSameNumericRules() {
        VArrayLong source = new VArrayLong();
        source.setV(new long[]{10L, 20L});
        assertEquals(Arrays.asList(10, 20), convert(source, VLListInteger.class).getV());

        VArrayLong overflow = new VArrayLong();
        overflow.setV(new long[]{2_147_483_648L});
        assertThrows(CopyException.class, () -> convert(overflow, VLListInteger.class));
    }

    @Test
    void bigDecimalAndBigIntegerCollectionConversion() {
        VLListBigInteger source = new VLListBigInteger();
        source.setV(Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2)));
        assertEquals(Arrays.asList(1L, 2L), convert(source, VLListLong.class).getV());
        assertEquals(Arrays.asList(new BigDecimal("1"), new BigDecimal("2")),
                convert(source, VLListBigDecimal.class).getV());
    }

    @Test
    void jsonLikeRoundTripAcrossRepresentations() {
        VWLong s1 = new VWLong();
        s1.setV(42L);
        assertEquals(42, convert(s1, VInt.class).getV());

        VInt s2 = new VInt();
        s2.setV(42);
        assertEquals(42L, convert(s2, VWLong.class).getV());

        VWInteger s3 = new VWInteger();
        s3.setV(42);
        assertEquals(42.0d, convert(s3, VWDouble.class).getV(), 0.001d);
        assertEquals(new BigDecimal("42"), convert(s2, VWBigDecimal.class).getV());

        VWBigDecimal s4 = new VWBigDecimal();
        s4.setV(new BigDecimal("42.0"));
        assertEquals(42, convert(s4, VInt.class).getV());

        VLListLong jsonIntsAsLong = new VLListLong();
        jsonIntsAsLong.setV(Arrays.asList(1L, 2L, 3L));
        assertEquals(Arrays.asList(1, 2, 3), convert(jsonIntsAsLong, VLListInteger.class).getV());
    }

    private static <S, T> T convert(S source, Class<T> targetClass) {
        return ConvertUtil.convert(source, targetClass);
    }

    // --- 标量：基本类型 ---

    @Getter
    @Setter
    static class VByte {
        private byte v;
    }

    @Getter
    @Setter
    static class VShort {
        private short v;
    }

    @Getter
    @Setter
    static class VInt {
        private int v;
    }

    @Getter
    @Setter
    static class VLong {
        private long v;
    }

    @Getter
    @Setter
    static class VFloat {
        private float v;
    }

    @Getter
    @Setter
    static class VDouble {
        private double v;
    }

    // --- 标量：包装类型 / 大数 ---

    @Getter
    @Setter
    static class VWByte {
        private Byte v;
    }

    @Getter
    @Setter
    static class VWShort {
        private Short v;
    }

    @Getter
    @Setter
    static class VWInteger {
        private Integer v;
    }

    @Getter
    @Setter
    static class VWLong {
        private Long v;
    }

    @Getter
    @Setter
    static class VWFloat {
        private Float v;
    }

    @Getter
    @Setter
    static class VWDouble {
        private Double v;
    }

    @Getter
    @Setter
    static class VWBigInteger {
        private BigInteger v;
    }

    @Getter
    @Setter
    static class VWBigDecimal {
        private BigDecimal v;
    }

    // --- 集合 / 数组 ---

    @Getter
    @Setter
    static class VLListByte {
        private List<Byte> v;
    }

    @Getter
    @Setter
    static class VLListInteger {
        private List<Integer> v;
    }

    @Getter
    @Setter
    static class VLListLong {
        private List<Long> v;
    }

    @Getter
    @Setter
    static class VLListFloat {
        private List<Float> v;
    }

    @Getter
    @Setter
    static class VLListDouble {
        private List<Double> v;
    }

    @Getter
    @Setter
    static class VLListBigInteger {
        private List<BigInteger> v;
    }

    @Getter
    @Setter
    static class VLListBigDecimal {
        private List<BigDecimal> v;
    }

    @Getter
    @Setter
    static class VArrayLong {
        private long[] v;
    }

    @Getter
    @Setter
    static class VArrayDouble {
        private double[] v;
    }
}
