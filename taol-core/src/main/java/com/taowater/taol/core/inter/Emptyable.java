package com.taowater.taol.core.inter;

/**
 * 可能为空的定义
 *
 * @author zhu56
 */
public interface Emptyable {

    /**
     * 实现此接口的类自行定义判空逻辑
     *
     * @return boolean
     */
    boolean isEmpty();
}
