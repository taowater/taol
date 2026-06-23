package com.test.benchmark;

import com.taowater.taol.core.util.CollUtil;
import com.test.bean.Bean1;
import com.test.bean.bench.CollectionBenchSource;
import com.test.bean.bench.NumericBenchSource;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@UtilityClass
public class BenchBeanFactory {

    @SneakyThrows
    public static Bean1 flatSource() {
        Bean1 bean = new Bean1();
        bean.setId(1L);
        bean.setName("jack");
        bean.setAge(123);
        bean.setList(CollUtil.list(1, 4, 6, 9));
        bean.setList2(new List[]{CollUtil.list(1), CollUtil.list(5), CollUtil.list(5)});
        for (int i = 1; i <= 100; i++) {
            invokeSetter(bean, Bean1.class, "setField" + i, String.class, "field" + i);
        }
        return bean;
    }

    public static NumericBenchSource numericSource() {
        NumericBenchSource source = new NumericBenchSource();
        source.setByteToLong((byte) 10);
        source.setByteToDouble((byte) 20);
        source.setShortToInt((short) 100);
        source.setShortToLong((short) 101);
        source.setShortToFloat((short) 102);
        source.setIntToLong(1_000);
        source.setIntToDouble(2_000);
        source.setIntToFloat(3_000);
        source.setLongToDouble(10_000L);
        source.setLongToFloat(20_000L);
        source.setLongToInt(30_000L);
        source.setFloatToDouble(3.5f);
        source.setByteWrapToLong((byte) 11);
        source.setIntWrapToDouble(1_001);
        source.setLongWrapToFloat(20_001L);
        source.setDoubleWrapToFloat(4.5d);
        source.setShortWrapToDouble((short) 103);
        source.setIntWrapToLong(1_002);
        return source;
    }

    public static CollectionBenchSource collectionSource() {
        CollectionBenchSource source = new CollectionBenchSource();
        source.setBytesToLongs(Arrays.asList((byte) 1, (byte) 2, (byte) 3));
        source.setShortsToInts(Arrays.asList((short) 10, (short) 20));
        source.setIntsToDoubles(Arrays.asList(100, 200, 300));
        source.setLongsToFloats(Arrays.asList(1_000L, 2_000L));
        source.setLongsToInts(Arrays.asList(1L, 2L, 3L));
        source.setSetShortToListLong(new LinkedHashSet<>(Arrays.asList((short) 7, (short) 8, (short) 9)));
        source.setLongsToDoubles(new long[]{1L, 2L, Long.MAX_VALUE});
        return source;
    }

    @SneakyThrows
    private static void invokeSetter(Object target, Class<?> clazz, String methodName,
                                     Class<?> paramType, Object value) {
        Method method = clazz.getMethod(methodName, paramType);
        method.invoke(target, value);
    }
}
