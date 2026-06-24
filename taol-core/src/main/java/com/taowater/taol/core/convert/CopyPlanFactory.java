package com.taowater.taol.core.convert;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 构建预编译拷贝计划。
 * <p>
 * 按 target 字段遍历，匹配 source 同名字段，为每对字段生成 {@link FieldCopyAction}：
 * <ol>
 *   <li>fast path：同类型引用 / 数值·char·boolean 标量（{@link NumericCopyActions}）</li>
 *   <li>fallback：{@link CopyValueConverter} 通用转换（含集合/数组元素）</li>
 * </ol>
 */
@UtilityClass
@SuppressWarnings("unchecked")
final class CopyPlanFactory {

    /** 扫描 target 字段，按同名匹配 source，组装字段级拷贝动作。 */
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

    /**
     * 单字段拷贝策略：先尝试 fast path，否则走通用 convert。
     * getter/setter 缺失则跳过该字段。
     */
    private static FieldCopyAction createAction(Class<?> sourceClass, Class<?> targetClass,
                                                FieldMetadata sourceField, FieldMetadata targetField) {
        Object getter = sourceField.ensureGetter(sourceClass);
        Object setter = targetField.ensureSetter(targetClass);
        if (getter == null || setter == null) {
            return null;
        }

        FieldCopyAction fastPath = resolveFastPath(sourceField, targetField, getter, setter);
        if (fastPath != null) {
            return fastPath;
        }
        return genericConvertAction(getter, setter, sourceField, targetField);
    }

    /**
     * fast path 优先级：
     * 1. 包装类型同泛型 → 直传
     * 2. 数值/char/boolean 标量 → {@link NumericCopyActions}（无 BigDecimal 热路径）
     * 3. 数值标量 fast path 未命中 → {@link CopyValueConverter#convertNumber} 兜底
     */
    private static FieldCopyAction resolveFastPath(FieldMetadata sourceField, FieldMetadata targetField,
                                                   Object getter, Object setter) {
        Class<?> sourceType = sourceField.getFieldClass();
        Class<?> targetType = targetField.getFieldClass();

        // 基本类型同名不走此分支（如 int→int），交给 NumericCopyActions
        if (Objects.equals(sourceField.getType(), targetField.getType()) && !sourceType.isPrimitive()) {
            return sameTypeAction(getter, setter);
        }

        if (!canUseNumericCopyActions(sourceField, targetField)) {
            return null;
        }

        FieldCopyAction action = NumericCopyActions.create(
                sourceType, targetType, getter, setter, sourceField.getName());
        if (action != null) {
            return action;
        }
        // 如 BigDecimal→Integer 等 NumericCopyActions 未覆盖的组合
        if (isScalarNumeric(sourceField)) {
            return numericConvertAction(getter, setter, sourceField, targetField);
        }
        return null;
    }

    private static boolean canUseNumericCopyActions(FieldMetadata sourceField, FieldMetadata targetField) {
        return (isScalarNumeric(sourceField) && isScalarNumeric(targetField))
                || (isScalarBooleanOrChar(sourceField) && isScalarBooleanOrChar(targetField));
    }

    /** 排除数组/集合，只处理标量字段。 */
    private static boolean isScalarField(FieldMetadata field) {
        Class<?> fieldClass = field.getFieldClass();
        return !fieldClass.isArray() && !Collection.class.isAssignableFrom(fieldClass);
    }

    private static boolean isScalarNumeric(FieldMetadata field) {
        return isScalarField(field) && CopyValueConverter.isNumericType(field.getType());
    }

    private static boolean isScalarBooleanOrChar(FieldMetadata field) {
        if (!isScalarField(field)) {
            return false;
        }
        Class<?> fieldClass = field.getFieldClass();
        return fieldClass == boolean.class || fieldClass == Boolean.class
                || fieldClass == char.class || fieldClass == Character.class;
    }

    /** 同类型引用字段：非 null 直传，不做转换。 */
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

    /**
     * 通用拷贝：运行时调用 {@link CopyValueConverter#convert}。
     * 转换不支持时静默跳过；数值类型则抛 {@link CopyException}。
     */
    private static FieldCopyAction genericConvertAction(Object getter, Object setter,
                                                        FieldMetadata sourceField, FieldMetadata targetField) {
        Function<Object, Object> objectGetter = (Function<Object, Object>) GetSetHelper.asFunctionGetter(getter);
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        Type sourceGeneric = sourceField.getType();
        Type targetGeneric = targetField.getType();
        Class<?> targetType = targetField.getFieldClass();
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

    /** 数值 fast path 未命中时的运行时数值转换（含 BigDecimal/BigInteger）。 */
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
