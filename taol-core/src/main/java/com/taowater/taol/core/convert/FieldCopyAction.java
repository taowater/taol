package com.taowater.taol.core.convert;

/**
 * 预编译的单字段拷贝动作，运行时仅做 getter →（可选转换）→ setter。
 */
@FunctionalInterface
interface FieldCopyAction {
    void copy(Object source, Object target);
}
