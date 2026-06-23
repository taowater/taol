package com.test;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.BeanCopier;
import com.taowater.taol.core.convert.ConvertUtil;
import com.test.bean.Bean1;
import com.test.bean.Bean2;
import com.test.bean.BeanMapper;
import com.test.bean.bench.CollectionBenchMapper;
import com.test.bean.bench.CollectionBenchSource;
import com.test.bean.bench.CollectionBenchTarget;
import com.test.bean.bench.NumericBenchMapper;
import com.test.bean.bench.NumericBenchSource;
import com.test.bean.bench.NumericBenchTarget;
import com.test.benchmark.BenchBeanFactory;
import com.test.benchmark.CopyBenchmark;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

/**
 * 拷贝正确性 + 分场景性能粗测（开发参考，不作 CI 门槛）。
 */
class ConvertTest {

    @Test
    void correctnessSample() {
        Bean1 source = BenchBeanFactory.flatSource();
        System.out.println("taol:" + ConvertUtil.convert(source, Bean2.class));
        System.out.println("hutool:" + BeanUtil.copyProperties(source, Bean2.class));
        Bean2 springTarget = new Bean2();
        BeanUtils.copyProperties(source, springTarget);
        System.out.println("spring:" + springTarget);
        Bean2 cglibTarget = new Bean2();
        BeanCopier.create(source, cglibTarget, null).copy();
        System.out.println("cglib:" + cglibTarget);
        System.out.println("mapstruct:" + BeanMapper.INSTANCE.to(source));
    }

    @Test
    void benchmarkFlatBean() {
        Bean1 source = BenchBeanFactory.flatSource();
        Bean2 target = new Bean2();
        BeanCopier<Bean2> hutoolCopier = BeanCopier.create(source, target, null);

        CopyBenchmark.beginSuite("Flat Bean (~100 String + ~100 int + 少量类型转换)");
        CopyBenchmark.beginSection("in-place");
        CopyBenchmark.measure("taol", () -> ConvertUtil.copy(source, target));
        CopyBenchmark.measure("hutool", hutoolCopier::copy);
        CopyBenchmark.measure("spring", () -> BeanUtils.copyProperties(source, target));
        CopyBenchmark.measure("cglib", hutoolCopier::copy);
        CopyBenchmark.measure("mapstruct", () -> BeanMapper.INSTANCE.update(source, target));

        CopyBenchmark.beginSection("new instance");
        CopyBenchmark.measure("taol", () -> ConvertUtil.convert(source, Bean2.class));
        CopyBenchmark.measure("hutool", () -> BeanUtil.copyProperties(source, Bean2.class));
        CopyBenchmark.measure("mapstruct", () -> BeanMapper.INSTANCE.to(source));
        System.out.println("spring/cglib: N/A（无直接 convert 到 Class 的 API）");
    }

    @Test
    void benchmarkNumericBean() {
        NumericBenchSource source = BenchBeanFactory.numericSource();
        NumericBenchTarget target = new NumericBenchTarget();
        NumericBenchTarget springCheck = new NumericBenchTarget();
        BeanUtils.copyProperties(source, springCheck);
        System.out.println("说明: Spring 跳过类型不匹配的字段，numeric 基准下几乎不执行转换（见 springCheck 默认值）");
        System.out.println("      taol 执行全部 18 个数值拓宽/窄化；对比 Spring 时请关注「真实转换」语义差异");
        System.out.println("springCheck.longToInt=" + springCheck.getLongToInt() + " (源为30000，默认应为0)");

        BeanCopier<NumericBenchTarget> hutoolCopier = BeanCopier.create(source, target, null);

        CopyBenchmark.beginSuite("Numeric Bean (18 个标量数值拓宽/窄化字段)");
        CopyBenchmark.beginSection("in-place");
        CopyBenchmark.measure("taol", () -> ConvertUtil.copy(source, target));
        CopyBenchmark.measure("hutool", hutoolCopier::copy);
        CopyBenchmark.measure("spring", () -> BeanUtils.copyProperties(source, target));
        CopyBenchmark.measure("cglib", hutoolCopier::copy);
        CopyBenchmark.measure("mapstruct", () -> NumericBenchMapper.INSTANCE.update(source, target));

        CopyBenchmark.beginSection("new instance");
        CopyBenchmark.measure("taol", () -> ConvertUtil.convert(source, NumericBenchTarget.class));
        CopyBenchmark.measure("hutool", () -> BeanUtil.copyProperties(source, NumericBenchTarget.class));
        CopyBenchmark.measure("mapstruct", () -> NumericBenchMapper.INSTANCE.to(source));
        System.out.println("spring/cglib: N/A（无直接 convert 到 Class 的 API）");
    }

    @Test
    void benchmarkCollectionBean() {
        CollectionBenchSource source = BenchBeanFactory.collectionSource();
        CollectionBenchTarget target = new CollectionBenchTarget();
        BeanCopier<CollectionBenchTarget> hutoolCopier = BeanCopier.create(source, target, null);

        CopyBenchmark.beginSuite("Collection Bean (List/Set/数组元素类型转换)");
        System.out.println("说明: mapstruct 忽略 setShortToListLong、longsToDoubles（无等价映射）");
        CopyBenchmark.beginSection("in-place");
        CopyBenchmark.measure("taol", () -> ConvertUtil.copy(source, target));
        CopyBenchmark.measure("hutool", hutoolCopier::copy);
        CopyBenchmark.measure("spring", () -> BeanUtils.copyProperties(source, target));
        CopyBenchmark.measure("cglib", hutoolCopier::copy);
        CopyBenchmark.measure("mapstruct", () -> CollectionBenchMapper.INSTANCE.update(source, target));

        CopyBenchmark.beginSection("new instance");
        CopyBenchmark.measure("taol", () -> ConvertUtil.convert(source, CollectionBenchTarget.class));
        CopyBenchmark.measure("hutool", () -> BeanUtil.copyProperties(source, CollectionBenchTarget.class));
        CopyBenchmark.measure("mapstruct", () -> CollectionBenchMapper.INSTANCE.to(source));
        System.out.println("spring/cglib: N/A（无直接 convert 到 Class 的 API）");
    }
}
