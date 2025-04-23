package com.taowater.taol.core.function;


import java.io.Serializable;
import java.util.function.BiConsumer;


@FunctionalInterface
public interface Consumer2<T1, T2> extends BiConsumer<T1, T2>, Serializable {
}