package com.taowater.taol.core.convert;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 字段元信息
 */
@Setter
@Getter
@EqualsAndHashCode
public class FieldMetadata {

    private String name;
    private Type type;
    private Class<?> fieldClass;
    private Method getterMethod;
    private Method setterMethod;

    private volatile Object getterAccessor;
    private volatile Object setterAccessor;

    public Object ensureGetter(Class<?> clazz) {
        if (getterAccessor != null) {
            return getterAccessor;
        }
        if (getterMethod == null) {
            return null;
        }
        synchronized (this) {
            if (getterAccessor != null) {
                return getterAccessor;
            }
            getterAccessor = GetSetHelper.buildGetterAccessor(clazz, getterMethod);
            return getterAccessor;
        }
    }

    public Object ensureSetter(Class<?> clazz) {
        if (setterAccessor != null) {
            return setterAccessor;
        }
        if (setterMethod == null) {
            return null;
        }
        synchronized (this) {
            if (setterAccessor != null) {
                return setterAccessor;
            }
            setterAccessor = GetSetHelper.buildSetterAccessor(clazz, setterMethod);
            return setterAccessor;
        }
    }

    public Function<?, ?> asFunctionGetter(Class<?> clazz) {
        return GetSetHelper.asFunctionGetter(ensureGetter(clazz));
    }

    public BiConsumer<?, ?> asBiConsumerSetter(Class<?> clazz) {
        return GetSetHelper.asBiConsumerSetter(ensureSetter(clazz));
    }
}
