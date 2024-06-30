package com.taowater.taol.core.function;

import java.io.Serializable;
import java.util.function.BiFunction;

/**
 * 序列化的BiFunction
 *
 * @author zhu56
 * @version 1.0
 * @date 2023/9/12 16:04
 */
@FunctionalInterface
public interface SerBiFunction<T, U, R> extends BiFunction<T, U, R>, Serializable {
}
