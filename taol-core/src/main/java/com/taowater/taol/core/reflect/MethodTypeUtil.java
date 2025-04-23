package com.taowater.taol.core.reflect;

import lombok.experimental.UtilityClass;

import java.lang.invoke.MethodType;
import java.util.Arrays;

@UtilityClass
public class MethodTypeUtil {

    public MethodType voidReturnType(Class<?>... parameterTypes) {
        return MethodType.methodType(void.class, parameterTypes);
    }

    public MethodType functionType(int paramNum) {
        Class<?>[] list = new Class<?>[paramNum];
        Arrays.fill(list, Object.class);
        return MethodType.methodType(Object.class, list);
    }

    public MethodType consumerType(int paramNum) {
        Class<?>[] list = new Class<?>[paramNum];
        Arrays.fill(list, Object.class);
        return voidReturnType(list);
    }
}
