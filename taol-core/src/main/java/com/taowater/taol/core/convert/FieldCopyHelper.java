package com.taowater.taol.core.convert;

import com.taowater.taol.core.function.*;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.*;

/**
 * 字段拷贝，8 大基本类型走无装箱 fast path
 */
@UtilityClass
@SuppressWarnings("unchecked")
public class FieldCopyHelper {

    @FunctionalInterface
    private interface PrimitiveCopier {
        void copy(Object source, Object target, Class<?> sourceClass, Class<?> targetClass,
                  FieldMetadata sourceField, FieldMetadata targetField, Class<?> targetType);
    }

    private static final Map<Class<?>, PrimitiveCopier> PRIMITIVE_COPIERS = new HashMap<>();
    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = new HashMap<>();

    static {
        PRIMITIVE_WRAPPERS.put(boolean.class, Boolean.class);
        PRIMITIVE_WRAPPERS.put(byte.class, Byte.class);
        PRIMITIVE_WRAPPERS.put(short.class, Short.class);
        PRIMITIVE_WRAPPERS.put(char.class, Character.class);
        PRIMITIVE_WRAPPERS.put(int.class, Integer.class);
        PRIMITIVE_WRAPPERS.put(long.class, Long.class);
        PRIMITIVE_WRAPPERS.put(float.class, Float.class);
        PRIMITIVE_WRAPPERS.put(double.class, Double.class);
        PRIMITIVE_COPIERS.put(boolean.class, FieldCopyHelper::copyBoolean);
        PRIMITIVE_COPIERS.put(byte.class, FieldCopyHelper::copyByte);
        PRIMITIVE_COPIERS.put(short.class, FieldCopyHelper::copyShort);
        PRIMITIVE_COPIERS.put(char.class, FieldCopyHelper::copyChar);
        PRIMITIVE_COPIERS.put(int.class, FieldCopyHelper::copyInt);
        PRIMITIVE_COPIERS.put(long.class, FieldCopyHelper::copyLong);
        PRIMITIVE_COPIERS.put(float.class, FieldCopyHelper::copyFloat);
        PRIMITIVE_COPIERS.put(double.class, FieldCopyHelper::copyDouble);
    }

    public static void copy(Object source, Object target, Class<?> sourceClass, Class<?> targetClass,
                            FieldMetadata sourceField, FieldMetadata targetField) {
        Class<?> sourceType = sourceField.getFieldClass();
        PrimitiveCopier copier = PRIMITIVE_COPIERS.get(sourceType);
        if (copier != null) {
            copier.copy(source, target, sourceClass, targetClass, sourceField, targetField, targetField.getFieldClass());
            return;
        }
        copyObject(source, target, sourceClass, targetClass, sourceField, targetField);
    }

    private static void copyBoolean(Object source, Object target, Class<?> sourceClass, Class<?> targetClass,
                                    FieldMetadata sourceField, FieldMetadata targetField, Class<?> targetType) {
        ToBooleanFunction<Object> getter = (ToBooleanFunction<Object>) sourceField.ensureGetter(sourceClass);
        if (getter == null) {
            return;
        }
        boolean value = getter.applyAsBoolean(source);
        if (targetType == boolean.class) {
            ObjBooleanConsumer<Object> setter = (ObjBooleanConsumer<Object>) targetField.ensureSetter(targetClass);
            if (setter != null) {
                setter.accept(target, value);
            }
            return;
        }
        writePrimitiveValue(target, targetClass, sourceField, targetField, value);
    }

    private static void copyByte(Object source, Object target, Class<?> sourceClass, Class<?> targetClass,
                                 FieldMetadata sourceField, FieldMetadata targetField, Class<?> targetType) {
        ToByteFunction<Object> getter = (ToByteFunction<Object>) sourceField.ensureGetter(sourceClass);
        if (getter == null) {
            return;
        }
        byte value = getter.applyAsByte(source);
        if (targetType == byte.class) {
            ObjByteConsumer<Object> setter = (ObjByteConsumer<Object>) targetField.ensureSetter(targetClass);
            if (setter != null) {
                setter.accept(target, value);
            }
            return;
        }
        writePrimitiveValue(target, targetClass, sourceField, targetField, value);
    }

    private static void copyShort(Object source, Object target, Class<?> sourceClass, Class<?> targetClass,
                                  FieldMetadata sourceField, FieldMetadata targetField, Class<?> targetType) {
        ToShortFunction<Object> getter = (ToShortFunction<Object>) sourceField.ensureGetter(sourceClass);
        if (getter == null) {
            return;
        }
        short value = getter.applyAsShort(source);
        if (targetType == short.class) {
            ObjShortConsumer<Object> setter = (ObjShortConsumer<Object>) targetField.ensureSetter(targetClass);
            if (setter != null) {
                setter.accept(target, value);
            }
            return;
        }
        writePrimitiveValue(target, targetClass, sourceField, targetField, value);
    }

    private static void copyChar(Object source, Object target, Class<?> sourceClass, Class<?> targetClass,
                                 FieldMetadata sourceField, FieldMetadata targetField, Class<?> targetType) {
        ToCharFunction<Object> getter = (ToCharFunction<Object>) sourceField.ensureGetter(sourceClass);
        if (getter == null) {
            return;
        }
        char value = getter.applyAsChar(source);
        if (targetType == char.class) {
            ObjCharConsumer<Object> setter = (ObjCharConsumer<Object>) targetField.ensureSetter(targetClass);
            if (setter != null) {
                setter.accept(target, value);
            }
            return;
        }
        writePrimitiveValue(target, targetClass, sourceField, targetField, value);
    }

    private static void copyInt(Object source, Object target, Class<?> sourceClass, Class<?> targetClass,
                                FieldMetadata sourceField, FieldMetadata targetField, Class<?> targetType) {
        ToIntFunction<Object> getter = (ToIntFunction<Object>) sourceField.ensureGetter(sourceClass);
        if (getter == null) {
            return;
        }
        int value = getter.applyAsInt(source);
        if (targetType == int.class) {
            ObjIntConsumer<Object> setter = (ObjIntConsumer<Object>) targetField.ensureSetter(targetClass);
            if (setter != null) {
                setter.accept(target, value);
            }
            return;
        }
        writePrimitiveValue(target, targetClass, sourceField, targetField, value);
    }

    private static void copyLong(Object source, Object target, Class<?> sourceClass, Class<?> targetClass,
                                 FieldMetadata sourceField, FieldMetadata targetField, Class<?> targetType) {
        ToLongFunction<Object> getter = (ToLongFunction<Object>) sourceField.ensureGetter(sourceClass);
        if (getter == null) {
            return;
        }
        long value = getter.applyAsLong(source);
        if (targetType == long.class) {
            ObjLongConsumer<Object> setter = (ObjLongConsumer<Object>) targetField.ensureSetter(targetClass);
            if (setter != null) {
                setter.accept(target, value);
            }
            return;
        }
        writePrimitiveValue(target, targetClass, sourceField, targetField, value);
    }

    private static void copyFloat(Object source, Object target, Class<?> sourceClass, Class<?> targetClass,
                                  FieldMetadata sourceField, FieldMetadata targetField, Class<?> targetType) {
        ToFloatFunction<Object> getter = (ToFloatFunction<Object>) sourceField.ensureGetter(sourceClass);
        if (getter == null) {
            return;
        }
        float value = getter.applyAsFloat(source);
        if (targetType == float.class) {
            ObjFloatConsumer<Object> setter = (ObjFloatConsumer<Object>) targetField.ensureSetter(targetClass);
            if (setter != null) {
                setter.accept(target, value);
            }
            return;
        }
        writePrimitiveValue(target, targetClass, sourceField, targetField, value);
    }

    private static void copyDouble(Object source, Object target, Class<?> sourceClass, Class<?> targetClass,
                                   FieldMetadata sourceField, FieldMetadata targetField, Class<?> targetType) {
        ToDoubleFunction<Object> getter = (ToDoubleFunction<Object>) sourceField.ensureGetter(sourceClass);
        if (getter == null) {
            return;
        }
        double value = getter.applyAsDouble(source);
        if (targetType == double.class) {
            ObjDoubleConsumer<Object> setter = (ObjDoubleConsumer<Object>) targetField.ensureSetter(targetClass);
            if (setter != null) {
                setter.accept(target, value);
            }
            return;
        }
        writePrimitiveValue(target, targetClass, sourceField, targetField, value);
    }

    private static void copyObject(Object source, Object target, Class<?> sourceClass, Class<?> targetClass,
                                   FieldMetadata sourceField, FieldMetadata targetField) {
        Function<Object, Object> getter = (Function<Object, Object>) GetSetHelper.asFunctionGetter(
                sourceField.ensureGetter(sourceClass));
        if (getter == null) {
            return;
        }
        Object value = getter.apply(source);
        if (value == null) {
            return;
        }
        writeConverted(target, targetClass, sourceField, targetField, value);
    }

    private static void writePrimitiveValue(Object target, Class<?> targetClass,
                                            FieldMetadata sourceField, FieldMetadata targetField, Object value) {
        if (isDirectlyAssignable(targetField.getFieldClass(), value)) {
            writeBoxed(target, targetClass, targetField, value);
            return;
        }
        writeConverted(target, targetClass, sourceField, targetField, value);
    }

    private static boolean isDirectlyAssignable(Class<?> targetFieldClass, Object value) {
        if (value == null) {
            return false;
        }
        Class<?> valueClass = value.getClass();
        if (targetFieldClass.isAssignableFrom(valueClass)) {
            return true;
        }
        if (targetFieldClass.isPrimitive()) {
            Class<?> wrapper = PRIMITIVE_WRAPPERS.get(targetFieldClass);
            return wrapper != null && Objects.equals(wrapper, valueClass);
        }
        return false;
    }

    private static void writeConverted(Object target, Class<?> targetClass,
                                       FieldMetadata sourceField, FieldMetadata targetField, Object value) {
        Object setterAccessor = targetField.ensureSetter(targetClass);
        if (setterAccessor == null) {
            return;
        }
        CopyValueConverter.ConvertedValue convertedValue = CopyValueConverter.convert(
                value, sourceField.getType(), targetField.getType(), sourceField.getName());
        if (!convertedValue.isSupported()) {
            if (value instanceof Number && CopyValueConverter.isNumericType(targetField.getType())) {
                throw new CopyException("Cannot copy " + sourceField.getName()
                        + ": unsupported numeric conversion from " + value.getClass().getName()
                        + " to " + targetField.getFieldClass().getName());
            }
            return;
        }
        writeBoxed(target, setterAccessor, convertedValue.getValue());
    }

    private static void writeBoxed(Object target, Class<?> targetClass, FieldMetadata targetField, Object value) {
        writeBoxed(target, targetField.ensureSetter(targetClass), value);
    }

    private static void writeBoxed(Object target, Object setterAccessor, Object value) {
        BiConsumer<Object, Object> setter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setterAccessor);
        if (setter != null) {
            setter.accept(target, value);
        }
    }
}
