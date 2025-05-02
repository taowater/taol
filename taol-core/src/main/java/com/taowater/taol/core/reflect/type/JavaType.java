package com.taowater.taol.core.reflect.type;

import java.lang.reflect.Type;

public interface JavaType<T> {

    Type getType();

    static <T> JavaType<T> of(Class<T> clazz) {
        return () -> clazz;
    }

    static <T> JavaType<T> of(TypeReference<T> typeRef) {
        return typeRef::getType;
    }
}