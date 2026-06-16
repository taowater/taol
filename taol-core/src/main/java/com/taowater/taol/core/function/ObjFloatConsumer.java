package com.taowater.taol.core.function;

/**
 * JDK 8 未提供 float 基本类型的 Obj*Consumer，用于 setter 无装箱调用。
 */
@FunctionalInterface
public interface ObjFloatConsumer<T> {
    void accept(T obj, float value);
}
