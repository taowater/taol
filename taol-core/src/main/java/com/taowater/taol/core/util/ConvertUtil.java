package com.taowater.taol.core.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.dromara.hutool.core.bean.BeanUtil;

/**
 * 转换工具
 *
 * @author zhu56
 * @date 2023/07/01 01:07
 */
@UtilityClass
public class ConvertUtil {

    /**
     * 转换
     *
     * @param source 源
     * @param tClazz 目标类型
     * @return {@link T}
     */
    @SneakyThrows
    public <S, T> T convert(S source, Class<T> tClazz) {
        T target = tClazz.getConstructor().newInstance();
        copy(source, target);
        return target;
    }


    /**
     * 复制
     *
     * @param source 源
     * @param target 目标
     */
    public <S, T> void copy(S source, T target) {
        BeanUtil.copyProperties(source, target, false);
    }

}
