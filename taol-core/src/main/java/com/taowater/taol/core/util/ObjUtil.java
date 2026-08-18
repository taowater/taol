package com.taowater.taol.core.util;

import lombok.experimental.UtilityClass;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

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
        Objects.requireNonNull(mapper);
        if (Objects.isNull(o)) {
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
        Objects.requireNonNull(consumer);
        if (Objects.isNull(o)) {
            return;
        }
        consumer.accept(o);
    }
}
