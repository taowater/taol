package com.taowater.taol.core.convert;

import com.taowater.taol.core.reflect.ClassUtil;
import com.taowater.taol.core.reflect.ReflectUtil;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 解析字段对应的 getter/setter 方法。
 * boolean 额外支持 isXxx；setter 支持链式返回（返回 this 的子类）。
 */
@UtilityClass
public class BeanMethodResolver {

    public static Method resolveGetter(Class<?> clazz, String fieldName) {
        Field field = ReflectUtil.getField(clazz, fieldName);
        if (field == null) {
            return null;
        }
        return resolveGetter(clazz, field);
    }

    /**
     * 优先 getXxx，boolean 再试 isXxx。
     */
    public static Method resolveGetter(Class<?> clazz, Field field) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();

        Method method = findMethod(clazz, ClassUtil.getGetMethodName(fieldName), fieldType);
        if (method != null) {
            return method;
        }

        if (fieldType == boolean.class || fieldType == Boolean.class) {
            method = findMethod(clazz, ClassUtil.getPrimitiveBooleanGetMethodName(fieldName),
                    fieldType == boolean.class ? boolean.class : Boolean.class);
            if (method != null) {
                return method;
            }
            if (fieldType == Boolean.class) {
                method = findMethod(clazz, ClassUtil.getGetMethodName(fieldName), Boolean.class);
                if (method != null) {
                    return method;
                }
            }
        }
        return null;
    }

    public static Method resolveSetter(Class<?> clazz, String fieldName) {
        Field field = ReflectUtil.getField(clazz, fieldName);
        if (field == null) {
            return null;
        }
        return resolveSetter(clazz, field);
    }

    /**
     * 优先 void setXxx；否则接受返回 this 的链式 setter。
     */
    public static Method resolveSetter(Class<?> clazz, Field field) {
        String setterName = ClassUtil.getSetMethodName(field.getName());
        Class<?> fieldType = field.getType();

        Method method = findMethod(clazz, setterName, void.class, fieldType);
        if (method != null) {
            return method;
        }

        for (Class<?> searchType = clazz; searchType != null; searchType = searchType.getSuperclass()) {
            try {
                Method candidate = searchType.getDeclaredMethod(setterName, fieldType);
                if (Modifier.isStatic(candidate.getModifiers())) {
                    continue;
                }
                Class<?> returnType = candidate.getReturnType();
                if (returnType != void.class && clazz.isAssignableFrom(returnType)) {
                    return candidate;
                }
            } catch (NoSuchMethodException ignored) {
                // continue
            }
        }
        return null;
    }

    /** 沿继承链查找，跳过 static 方法。 */
    private static Method findMethod(Class<?> clazz, String name, Class<?> returnType, Class<?>... parameterTypes) {
        for (Class<?> searchType = clazz; searchType != null; searchType = searchType.getSuperclass()) {
            try {
                Method method = searchType.getDeclaredMethod(name, parameterTypes);
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (returnType == void.class) {
                    if (method.getReturnType() == void.class) {
                        return method;
                    }
                    continue;
                }
                if (returnType.isAssignableFrom(method.getReturnType())) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // continue
            }
        }
        return null;
    }
}
