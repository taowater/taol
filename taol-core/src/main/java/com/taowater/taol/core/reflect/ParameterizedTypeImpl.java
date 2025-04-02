package com.taowater.taol.core.reflect;


import lombok.Getter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 参数化类型实体
 *
 * @author zhu56
 * @date 2025/03/28 23:54
 */
@Getter
public class ParameterizedTypeImpl implements ParameterizedType {
    private final Type[] actualTypeArguments;
    private final Class<?> rawType;
    private final Type ownerType;

    public ParameterizedTypeImpl(Type[] actualTypeArguments, Class<?> rawType, Type ownerType) {
        this.actualTypeArguments = actualTypeArguments;
        this.rawType = rawType;
        this.ownerType = ownerType != null ? ownerType : rawType.getDeclaringClass();
    }
}