package com.taowater.taol.core.function;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * 序列化的提供者
 *
 * @author zhu56
 * @version 1.0
 * @date 2022/4/29 16:27
 */
@FunctionalInterface
public interface SerSupplier<T> extends Supplier<T>, Serializable {
}
