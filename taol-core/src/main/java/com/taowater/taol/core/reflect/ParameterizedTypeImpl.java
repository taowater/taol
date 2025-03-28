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

    public String toString() {
        StringBuilder var1 = new StringBuilder();
        if (this.ownerType != null) {
            if (this.ownerType instanceof Class) {
                var1.append(((Class) this.ownerType).getName());
            } else {
                var1.append(this.ownerType.toString());
            }

            var1.append("$");
            if (this.ownerType instanceof ParameterizedTypeImpl) {
                var1.append(this.rawType.getName().replace(((ParameterizedTypeImpl) this.ownerType).rawType.getName() + "$", ""));
            } else {
                var1.append(this.rawType.getSimpleName());
            }
        } else {
            var1.append(this.rawType.getName());
        }

        if (this.actualTypeArguments != null && this.actualTypeArguments.length > 0) {
            var1.append("<");
            boolean var2 = true;

            for (Type var6 : this.actualTypeArguments) {
                if (!var2) {
                    var1.append(", ");
                }

                var1.append(var6.getTypeName());
                var2 = false;
            }

            var1.append(">");
        }

        return var1.toString();
    }

}