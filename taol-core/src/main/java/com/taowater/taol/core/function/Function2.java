package com.taowater.taol.core.function;


import java.io.Serializable;
import java.util.function.BiFunction;
 
@FunctionalInterface
public interface Function2<T1, T2, R> extends Serializable, BiFunction<T1, T2, R> {
}