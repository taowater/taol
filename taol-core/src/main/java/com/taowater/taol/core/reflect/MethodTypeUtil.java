package com.taowater.taol.core.reflect;

import lombok.experimental.UtilityClass;

import java.lang.invoke.MethodType;

@UtilityClass
public class MethodTypeUtil {

    public MethodType voidReturnType(Class<?>... parameterTypes) {
        return MethodType.methodType(void.class, parameterTypes);
    }
}
