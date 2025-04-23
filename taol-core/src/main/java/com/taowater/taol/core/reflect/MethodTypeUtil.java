package com.taowater.taol.core.reflect;

import lombok.experimental.UtilityClass;

import java.lang.invoke.MethodType;

@UtilityClass
public class MethodTypeUtil {

    public MethodType voidReturnType(Class<?>... parameterTypes) {
        return MethodType.methodType(void.class, parameterTypes);
    }

    public MethodType function1Type() {
        return MethodType.methodType(Object.class, Object.class);
    }

    public MethodType consumer2Type() {
        return voidReturnType(Object.class, Object.class);
    }
}
