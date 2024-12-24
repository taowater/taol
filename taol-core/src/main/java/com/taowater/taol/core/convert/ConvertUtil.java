package com.taowater.taol.core.convert;


import com.tuyang.beanutils.BeanCopyUtils;
import lombok.experimental.UtilityClass;

/**
 * 转换工具类
 *
 * @author zhu56
 * @date 2023/05/03 23:29
 */
@UtilityClass
public class ConvertUtil {


    /**
     * 转换
     *
     * @param source 源
     * @param tClazz t clazz
     * @return {@link T}
     */
    public <S, T> T convert(S source, Class<T> tClazz) {
        return BeanCopyUtils.copyBean(source, tClazz);
    }

    /**
     * 拷贝
     *
     * @param source 源
     * @param target 目标
     */
    public <S, T> void copy(S source, T target) {
        BeanCopyUtils.copyBean(source, target);
    }

}
