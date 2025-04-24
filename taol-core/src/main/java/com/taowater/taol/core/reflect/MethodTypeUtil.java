package com.taowater.taol.core.reflect;

import com.taowater.taol.core.util.CollUtil;
import lombok.experimental.UtilityClass;

import java.lang.invoke.MethodType;

/**
 * 反射用MethodType构建相关
 *
 * @author zhu56
 */
@UtilityClass
public class MethodTypeUtil {

    public MethodType voidReturnType(Class<?>... parameterTypes) {
        return MethodType.methodType(void.class, parameterTypes);
    }

    public MethodType functionType(int paramNum) {
        return MethodType.methodType(Object.class, CollUtil.arr(Object.class, paramNum));
    }

    public MethodType consumerType(int paramNum) {
        return voidReturnType(CollUtil.arr(Object.class, paramNum));
    }
}
