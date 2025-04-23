package com.taowater.taol.core.convert;

import lombok.Data;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 字段元信息
 *
 * @author zhu56
 */
@Data
public class FieldMetadata {

    /**
     * 字段名称
     */
    private String name;
    /**
     * 字段类型
     */
    private Class<?> type;
    /**
     * 字段 getter
     */
    private Function<?, ?> getter;
    /**
     * 字段 setter
     */
    private BiConsumer<?, ?> setter;
}
