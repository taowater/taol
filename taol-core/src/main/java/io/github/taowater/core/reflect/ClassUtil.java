package io.github.taowater.core.reflect;

import io.github.taowater.core.util.EmptyUtil;
import lombok.experimental.UtilityClass;
import org.dromara.hutool.core.array.ArrayUtil;
import org.dromara.hutool.core.reflect.TypeUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * class工具类
 *
 * @author 朱滔
 * @date 2021/9/19 15:35
 */
@UtilityClass
public class ClassUtil {
    /**
     * 获得指定类型指定位置的传入泛型真实类型
     *
     * @param clazz 类型
     * @param index 泛型参数位置
     * @return {@link Class}<{@link N}>
     */
    @SuppressWarnings("unchecked")
    public <T, N> Class<N> getGenericType(Class<T> clazz, int index) {
        return Optional.of(clazz)
                .map(Class::getGenericSuperclass)
                .map(ClassUtil::getActualTypeArguments)
                .map(a -> (Class<N>) ArrayUtil.get(a, index))
                .orElse(null);
    }

    /**
     * 获取某类指定接口的泛型类型
     *
     * @param clazz          clazz
     * @param interfaceClazz 接口clazz
     * @param index          指数
     * @return {@link Class}<{@link N}>
     */
    @SuppressWarnings("unchecked")
    public <T, N> Class<N> getInterfaceGenericType(Class<T> clazz, Class<?> interfaceClazz, int index) {
        if (EmptyUtil.isHadEmpty(clazz, interfaceClazz) || !interfaceClazz.isInterface()) {
            return null;
        }

        return Stream.of(clazz.getGenericInterfaces())
                .filter(e -> interfaceClazz.isAssignableFrom(TypeUtil.getClass(((ParameterizedType) e).getRawType())))
                .findFirst()
                .map(ClassUtil::getActualTypeArguments)
                .map(a -> (Class<N>) ArrayUtil.get(a, index))
                .orElse(null);
    }

    /**
     * 得到实际类型参数
     *
     * @param type 类型
     * @return {@link Type[]}
     */
    private Type[] getActualTypeArguments(Type type) {
        return Optional.of(type).map(e -> ((ParameterizedType) e).getActualTypeArguments()).orElse(null);
    }

    /**
     * 根据类型创建实例
     *
     * @param clazz clazz
     * @return {@link T}
     */
    public <T> T newInstance(Class<T> clazz) {
        return Optional.of(clazz).map(c -> {
            try {
                return c.getConstructor();
            } catch (NoSuchMethodException e) {
                return null;
            }
        }).map(c -> {
            try {
                return c.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * 创建带斜杠的全限定类名
     *
     * @param clazz clazz
     * @return {@link String}
     */
    public String getSlashName(Class<?> clazz) {
        return clazz.getName().replaceAll("\\.", "/");
    }

    public Class<?> fromName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
