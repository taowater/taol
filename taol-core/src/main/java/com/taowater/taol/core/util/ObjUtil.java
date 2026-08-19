package com.taowater.taol.core.util;

import lombok.experimental.UtilityClass;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 对象相关工具
 */
@UtilityClass
public class ObjUtil {

    /**
     *
     * 将目标对象转换
     *
     * @param o      对象
     * @param mapper 转换方法
     *
     */
    public <T> T get(Object o, Function<Object, T> mapper) {
        return get(o, mapper, null);
    }

    /**
     * 将目标对象转换，对象为空时按需抛出异常
     *
     * @param o             对象
     * @param mapper        转换方法，不可为 null
     * @param throwSupplier 空对象时提供异常，可为 null
     * @param <T>           转换结果类型
     * @return 对象为空且未提供异常时返回 null，否则返回转换结果
     */
    public <T> T get(Object o, Function<Object, T> mapper, Supplier<RuntimeException> throwSupplier) {
        Objects.requireNonNull(mapper);
        if (Objects.isNull(o)) {
            if (Objects.nonNull(throwSupplier)) {
                throw throwSupplier.get();
            }
            return null;
        }
        return mapper.apply(o);
    }

    /**
     *
     * 使用目标对象消费
     *
     * @param o        对象
     * @param consumer 消费方法
     *
     */
    public void run(Object o, Consumer<Object> consumer) {
        run(o, consumer, null);
    }

    /**
     * 使用目标对象消费，对象为空时按需抛出异常
     *
     * @param o             对象
     * @param consumer      消费方法，不可为 null
     * @param throwSupplier 空对象时提供异常，可为 null
     */
    public void run(Object o, Consumer<Object> consumer, Supplier<RuntimeException> throwSupplier) {
        Objects.requireNonNull(consumer);
        if (Objects.isNull(o)) {
            if (Objects.nonNull(throwSupplier)) {
                throw throwSupplier.get();
            }
            return;
        }
        consumer.accept(o);
    }
}
