package com.taowater.taol.core.function;

/**
 * JDK 8 未提供 byte 基本类型的 Obj*Consumer，用于 setter 无装箱调用。
 */
@FunctionalInterface
public interface ObjByteConsumer<T> {
    void accept(T obj, byte value);
}
