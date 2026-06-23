package com.taowater.taol.core.convert;

/**
 * 预编译的单字段拷贝动作。
 */
@FunctionalInterface
interface FieldCopyAction {
    void copy(Object source, Object target);
}
