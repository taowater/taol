package com.taowater.taol.core.bo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class Tuple<T1, T2> implements Serializable {

    public T1 left;

    public T2 right;

    public static <T1, T2> Tuple<T1, T2> of(T1 left, T2 right) {
        return new Tuple<>(left, right);
    }
}
