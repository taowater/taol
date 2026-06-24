package com.taowater.taol.core.convert;

import lombok.experimental.StandardException;

/**
 * 拷贝失败异常：数值窄化溢出、不支持的数值转换、基本类型数组写入 null 等。
 */
@StandardException
public class CopyException extends RuntimeException {
}
