package com.taowater.taol.core.convert;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 单字段元信息：泛型 {@link #type}、原始 {@link #fieldClass}、getter/setter 及懒加载 accessor。
 */
@Setter
@Getter
@EqualsAndHashCode
public class FieldMetadata {

    private String name;
    /** 字段声明泛型，用于集合/数组元素类型推断。 */
    private Type type;
    private Class<?> fieldClass;
    private Method getterMethod;
    private Method setterMethod;

    /** 预编译 accessor，double-checked locking 懒初始化。 */
    private volatile Object getterAccessor;
    private volatile Object setterAccessor;

    /** 构建并缓存 getter Lambda（{@link GetSetHelper}）。 */
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

    /** 构建并缓存 setter Lambda（{@link GetSetHelper}）。 */
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
