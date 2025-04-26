package com.taowater.taol.core.reflect.type;


import lombok.Getter;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * 参数化类型实体
 *
 * @author zhu56
 */
@Getter
public class ParameterizedTypeImpl implements ParameterizedType {
    private final Type[] actualTypeArguments;
    private final Class<?> rawType;
    private final Type ownerType;

    public ParameterizedTypeImpl(Class<?> rawType, Type[] actualTypeArguments, Type ownerType) {
        this.actualTypeArguments = actualTypeArguments;
        this.rawType = rawType;
        this.ownerType = ownerType != null ? ownerType : rawType.getDeclaringClass();
        this.validateConstructorArguments();
    }

    private void validateConstructorArguments() {
        TypeVariable<? extends Class<?>>[] formals = this.rawType.getTypeParameters();
        if (formals.length != this.actualTypeArguments.length) {
            throw new MalformedParameterizedTypeException();
        }
    }

}