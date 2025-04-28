package com.taowater.taol.core.convert.function;

import com.taowater.taol.core.reflect.ClassUtil;

import java.lang.invoke.*;

@FunctionalInterface
public interface IntGetter<T> extends Getter<T> {

    int apply(T t);

    @SuppressWarnings("unchecked")
    static <T> IntGetter<T> build(Class<T> targetClass, String fieldName) {
        try {

            String getterName = ClassUtil.getGetMethodName(fieldName);

            // 2. 查找方法
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.findVirtual(targetClass, getterName, MethodType.methodType(int.class));

            // 3. 生成Lambda
            CallSite callSite = LambdaMetafactory.metafactory(
                    lookup,
                    "apply",
                    MethodType.methodType(IntGetter.class),
                    MethodType.methodType(int.class, Object.class),
                    handle,
                    MethodType.methodType(int.class, targetClass)
            );
            return (IntGetter<T>) callSite.getTarget().invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create getter for field: " + fieldName, e);
        }
    }
}
