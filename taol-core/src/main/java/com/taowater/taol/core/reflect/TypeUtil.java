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

    /**
     * 获取某个类继承树上的某个祖先类的泛型类型
     *
     * @param type        类型
     * @param genericType 泛型类型
     * @return {@link Type }
     */
    public static Type getTypeArgument(Type type, Class<?> genericType) {
        return getTypeArgument(type, genericType, 0);
    }

    /**
     * 获取某个类继承树上的某个祖先类的泛型类型
     *
     * @param type        类型
     * @param genericType 泛型类型
     * @param index       泛型参数下标记
     * @return {@link Type }
     */
    public static Type getTypeArgument(Type type, Class<?> genericType, int index) {
        Type[] typeArguments = getTypeArguments(type, genericType);
        if (null == typeArguments || typeArguments.length <= index) {
            return null;
        }
        return typeArguments[index];
    }


    /**
     * 获取某个类继承树上的某个祖先类的泛型类型列表
     *
     * @param type        类型
     * @param genericType 泛型类型
     * @return {@link Type[] }
     */
    public static Type[] getTypeArguments(Type type, Class<?> genericType) {
        if (null == type || null == genericType) {
            return null;
        }
        ParameterizedType parameterizedType = toParameterizedType(type, genericType);
        return Optional.ofNullable(parameterizedType).map(ParameterizedType::getActualTypeArguments).orElse(null);
    }

    /**
     * 获取某个类继承树上的某个祖先类的参数化类型
     *
     * @param type        类型
     * @param genericType 泛型类型
     * @return {@link ParameterizedType }
     */
    public static ParameterizedType toParameterizedType(Type type, Class<?> genericType) {
        Map<Class<?>, ParameterizedType> map = mapGenerics((Class<?>) type);
        if (EmptyUtil.isEmpty(map)) {
            return null;
        }
        return map.get(genericType);
    }

    /**
     * 递归获取某个类继承树上的所有类型和参数类型的映射
     *
     * @param clazz 克拉兹
     * @return {@link Map }<{@link Class }<{@link ? }>, {@link ParameterizedType }>
     */
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

    /**
     * 获取某个类型本身的参数化类型信息
     *
     * @param type 类型
     * @return {@link Map }<{@link Class }<{@link ? }>, {@link ParameterizedType }>
     */
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
     * 获取实际类型
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
