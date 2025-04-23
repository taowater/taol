package com.taowater.taol.core.convert;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 字段元信息
 *
 * @author zhu56
 */
@Getter
@Setter
@EqualsAndHashCode
public class FieldMetadata {

    /**
     * 字段名称
     */
    private String name;
    /**
     * 字段类型
     */
    private Type type;
    /**
     * 字段 getter
     */
    private Function<?, ?> getter;

    /**
     * 字段 setter
     */
    private BiConsumer<?, ?> setter;
}
