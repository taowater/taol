package com.taowater.taol.core.reflect;

import com.taowater.taol.core.util.EmptyUtil;
import lombok.experimental.UtilityClass;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.stream.Stream;

@UtilityClass
public class TypeUtil {

    public static Type getTypeArgument(Type type, Class<?> interfaceType, int index) {
        final Type[] typeArguments = getTypeArguments(type, interfaceType);
        if (null == typeArguments || typeArguments.length <= index) {
            return null;
        }
        return typeArguments[index];
    }


    public static Type[] getTypeArguments(Type type, Class<?> interfaceType) {
        if (null == type || null == interfaceType) {
            return null;
        }
        ParameterizedType parameterizedType = toParameterizedType(type, interfaceType);
        return Optional.ofNullable(parameterizedType).map(ParameterizedType::getActualTypeArguments).orElse(null);
    }

    public static ParameterizedType toParameterizedType(Type type, Class<?> interfaceType) {
        Map<Class<?>, ParameterizedType> map = mapGenerics((Class<?>) type);
        if (EmptyUtil.isEmpty(map)) {
            return null;
        }
        return map.get(interfaceType);
    }

    public static Map<Class<?>, ParameterizedType> mapGenerics(Class<?> clazz) {
        Map<Class<?>, ParameterizedType> result = new HashMap<>();
        // 继承的父类
        Type superClass = clazz.getGenericSuperclass();
        Map<Class<?>, ParameterizedType> map;
        if (null != superClass && !Object.class.equals(superClass)) {
            map = getGenerics(superClass);
            if (EmptyUtil.isNotEmpty(map)) {
                result.putAll(map);
            }
        }
        // 实现的接口
        for (Type inter : clazz.getGenericInterfaces()) {
            map = getGenerics(inter);
            if (EmptyUtil.isNotEmpty(map)) {
                result.putAll(map);
            }
        }
        return result;
    }

    private Map<Class<?>, ParameterizedType> getGenerics(Type type) {
        Map<Class<?>, ParameterizedType> result = new HashMap<>();
        Map<Class<?>, ParameterizedType> map;
        if (type instanceof Class<?>) {
            map = mapGenerics((Class<?>) type);
            if (EmptyUtil.isNotEmpty(map)) {
                result.putAll(map);
            }
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedInter = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) (parameterizedInter).getRawType();
            result.put(rawType, parameterizedInter);
            map = mapGenerics(rawType);
            if (EmptyUtil.isNotEmpty(map)) {
                map.forEach((k, v) -> {
                    result.put(k, getRealParameterizedType(parameterizedInter, v));
                });
            }
        }
        return result;
    }

    /**
     * 将子类传递给父类的类型参数解析出来，构造一个真参数化类型
     *
     * @param type       类型
     * @param parentType 父类型
     * @return {@link ParameterizedType }
     */
    private static ParameterizedType getRealParameterizedType(ParameterizedType type, ParameterizedType parentType) {
        Type[] arguments = parentType.getActualTypeArguments();
        if (EmptyUtil.isEmpty(arguments)) {
            return parentType;
        }
        if (Stream.of(arguments).anyMatch(e -> e instanceof TypeVariable)) {
            List<Type> realTypes = getRealTypes(parentType, ((Class<?>) type.getRawType()).getTypeParameters(), type.getActualTypeArguments());
            return new ParameterizedTypeImpl(realTypes.toArray(new Type[0]), (Class<?>) parentType.getRawType(), parentType.getOwnerType());
        }
        return parentType;
    }


    /**
     * 获取实数类型
     *
     * @param type       类型
     * @param typeParams 类型参数
     * @param actualArgs 实际类型
     * @return {@link List }<{@link Type }>
     */
    private static List<Type> getRealTypes(Type type, TypeVariable<?>[] typeParams, Type[] actualArgs) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        List<Type> resolvedArgs = new ArrayList<>();
        for (Type typeArgument : typeArguments) {
            if (typeArgument instanceof TypeVariable) {
                // 找到类型变量在子类中的位置
                for (int i = 0; i < typeParams.length; i++) {
                    if (typeParams[i].equals(typeArgument)) {
                        resolvedArgs.add(actualArgs[i]);
                        break;
                    }
                }
            } else {
                resolvedArgs.add(typeArgument);
            }
        }
        return resolvedArgs;
    }
}
