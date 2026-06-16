package com.taowater.taol.core.function;

/**
 * JDK 8 未提供 byte 基本类型的 To*Function，用于 getter 无装箱调用。
 */
@FunctionalInterface
public interface ToByteFunction<T> {
    byte applyAsByte(T value);
}
