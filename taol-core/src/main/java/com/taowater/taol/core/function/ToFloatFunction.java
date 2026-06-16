package com.taowater.taol.core.function;

/**
 * JDK 8 未提供 float 基本类型的 To*Function，用于 getter 无装箱调用。
 */
@FunctionalInterface
public interface ToFloatFunction<T> {
    float applyAsFloat(T value);
}
