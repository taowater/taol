package com.taowater.taol.core.convert.function;

import com.taowater.taol.core.reflect.ClassUtil;

import java.lang.invoke.*;
import java.util.function.Function;

public interface StringGetter<T> extends Function<T, String> {

    @Override
    String apply(T t);

    @SuppressWarnings("unchecked")
    static <T> StringGetter<T> build(Class<T> targetClass, String fieldName) {
        try {

            String getterName = ClassUtil.getGetMethodName(fieldName);

            // 2. 查找方法
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.findVirtual(targetClass, getterName, MethodType.methodType(String.class));

            // 3. 生成Lambda
            CallSite callSite = LambdaMetafactory.metafactory(
                    lookup,
                    "apply",
                    MethodType.methodType(StringGetter.class),
                    MethodType.methodType(String.class, Object.class),
                    handle,
                    MethodType.methodType(String.class, targetClass)
            );
            return (StringGetter<T>) callSite.getTarget().invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create getter for field: " + fieldName, e);
        }
    }
}
