package com.taowater.taol.core.function;


import java.io.Serializable;
import java.util.function.Function;


@FunctionalInterface
public interface Function1<T1, R> extends Serializable, Function<T1, R> {
}