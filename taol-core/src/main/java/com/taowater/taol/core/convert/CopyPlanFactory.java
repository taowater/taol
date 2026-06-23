package com.taowater.taol.core.convert;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 构建预编译拷贝计划：预解析 accessor，数值拓宽走无 BigDecimal 的 fast path。
 */
final class CopyPlanFactory {

    private CopyPlanFactory() {
    }

    static BeanCopyPlan build(Class<?> sourceClass, Class<?> targetClass) {
        BeanMetadata sourceMetadata = BeanMetadata.of(sourceClass);
        BeanMetadata targetMetadata = BeanMetadata.of(targetClass);
        List<FieldCopyAction> actions = new ArrayList<>(targetMetadata.getFieldMap().size());
        for (FieldMetadata targetField : targetMetadata.getFieldMap().values()) {
            FieldMetadata sourceField = sourceMetadata.getField(targetField.getName());
            if (sourceField == null) {
                continue;
            }
            FieldCopyAction action = createAction(sourceClass, targetClass, sourceField, targetField);
            if (action != null) {
                actions.add(action);
            }
        }
        return BeanCopyPlan.create(actions);
    }

    private static FieldCopyAction createAction(Class<?> sourceClass, Class<?> targetClass,
                                                FieldMetadata sourceField, FieldMetadata targetField) {
        Class<?> sourceType = sourceField.getFieldClass();
        Class<?> targetType = targetField.getFieldClass();
        Object getter = sourceField.ensureGetter(sourceClass);
        Object setter = targetField.ensureSetter(targetClass);
        if (getter == null || setter == null) {
            return null;
        }

        Type sourceGeneric = sourceField.getType();
        Type targetGeneric = targetField.getType();

        if (Objects.equals(sourceGeneric, targetGeneric) && !sourceType.isPrimitive()) {
            return sameTypeAction(getter, setter);
        }

        if (isScalarNumeric(sourceField) && isScalarNumeric(targetField)) {
            FieldCopyAction numeric = NumericCopyActions.create(
                    sourceType, targetType, getter, setter, sourceField.getName());
            if (numeric != null) {
                return numeric;
            }
            return numericConvertAction(getter, setter, sourceField, targetField);
        }

        Function<Object, Object> objectGetter = (Function<Object, Object>) GetSetHelper.asFunctionGetter(getter);
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        String fieldName = sourceField.getName();
        return (source, target) -> {
            Object value = objectGetter.apply(source);
            if (value == null) {
                return;
            }
            CopyValueConverter.ConvertedValue converted = CopyValueConverter.convert(
                    value, sourceGeneric, targetGeneric, fieldName);
            if (!converted.isSupported()) {
                if (value instanceof Number) {
                    throw new CopyException("Cannot copy " + fieldName + ": unsupported numeric conversion from "
                            + value.getClass().getName() + " to " + targetType.getName());
                }
                return;
            }
            objectSetter.accept(target, converted.getValue());
        };
    }

    private static boolean isScalarNumeric(FieldMetadata field) {
        Class<?> fieldClass = field.getFieldClass();
        if (fieldClass.isArray() || Collection.class.isAssignableFrom(fieldClass)) {
            return false;
        }
        return CopyValueConverter.isNumericType(field.getType());
    }

    private static FieldCopyAction sameTypeAction(Object getter, Object setter) {
        Function<Object, Object> objectGetter = (Function<Object, Object>) GetSetHelper.asFunctionGetter(getter);
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        return (source, target) -> {
            Object value = objectGetter.apply(source);
            if (value != null) {
                objectSetter.accept(target, value);
            }
        };
    }

    private static FieldCopyAction numericConvertAction(Object getter, Object setter,
                                                        FieldMetadata sourceField, FieldMetadata targetField) {
        Function<Object, Object> objectGetter = (Function<Object, Object>) GetSetHelper.asFunctionGetter(getter);
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        Class<?> targetWrapper = CopyValueConverter.wrapClass(targetField.getFieldClass());
        String fieldName = sourceField.getName();
        return (source, target) -> {
            Object value = objectGetter.apply(source);
            if (value == null) {
                return;
            }
            objectSetter.accept(target, CopyValueConverter.convertNumber(
                    (Number) value, targetWrapper, fieldName));
        };
    }
}