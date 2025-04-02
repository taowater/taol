package com.taowater.taol.core.function;


import java.io.Serializable;
import java.util.function.Supplier;


@FunctionalInterface
public interface Function0<R> extends Serializable, Supplier<R> {
}