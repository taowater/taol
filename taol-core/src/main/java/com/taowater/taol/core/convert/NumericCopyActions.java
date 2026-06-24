package com.taowater.taol.core.convert;

import com.taowater.taol.core.function.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.*;

/**
 * 标量 fast path：基本类型 / 包装类型之间的拷贝，避免运行时 {@link CopyValueConverter} 分发与装箱。
 * <p>
 * 由 {@link CopyPlanFactory#resolveFastPath} 调用，按 source/target 类型组合分派：
 * primitive↔primitive、primitive→wrapper、wrapper→primitive、wrapper→wrapper。
 * 窄化溢出通过 {@link CopyValueConverter} 的 xxxFromXxx 方法抛 {@link CopyException}。
 */
@SuppressWarnings("unchecked")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class NumericCopyActions {

    @FunctionalInterface
    private interface SamePrimitiveFactory {
        FieldCopyAction create(Object getter, Object setter);
    }

    @FunctionalInterface
    private interface BoxActionFactory {
        FieldCopyAction create(Class<?> sourceType, Object getter, Object setter, String fieldName);
    }

    private static final Map<Class<?>, SamePrimitiveFactory> SAME_PRIMITIVE = new HashMap<>();
    private static final Map<Class<?>, BoxActionFactory> BOX_ACTIONS = new HashMap<>();

    static {
        SAME_PRIMITIVE.put(boolean.class, (getter, setter) -> {
            ToBooleanFunction<Object> g = (ToBooleanFunction<Object>) getter;
            ObjBooleanConsumer<Object> s = (ObjBooleanConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsBoolean(src));
        });
        SAME_PRIMITIVE.put(char.class, (getter, setter) -> {
            ToCharFunction<Object> g = (ToCharFunction<Object>) getter;
            ObjCharConsumer<Object> s = (ObjCharConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsChar(src));
        });
        SAME_PRIMITIVE.put(byte.class, (getter, setter) -> {
            ToByteFunction<Object> g = (ToByteFunction<Object>) getter;
            ObjByteConsumer<Object> s = (ObjByteConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsByte(src));
        });
        SAME_PRIMITIVE.put(short.class, (getter, setter) -> {
            ToShortFunction<Object> g = (ToShortFunction<Object>) getter;
            ObjShortConsumer<Object> s = (ObjShortConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsShort(src));
        });
        SAME_PRIMITIVE.put(int.class, (getter, setter) -> {
            ToIntFunction<Object> g = (ToIntFunction<Object>) getter;
            ObjIntConsumer<Object> s = (ObjIntConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsInt(src));
        });
        SAME_PRIMITIVE.put(long.class, (getter, setter) -> {
            ToLongFunction<Object> g = (ToLongFunction<Object>) getter;
            ObjLongConsumer<Object> s = (ObjLongConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsLong(src));
        });
        SAME_PRIMITIVE.put(float.class, (getter, setter) -> {
            ToFloatFunction<Object> g = (ToFloatFunction<Object>) getter;
            ObjFloatConsumer<Object> s = (ObjFloatConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsFloat(src));
        });
        SAME_PRIMITIVE.put(double.class, (getter, setter) -> {
            ToDoubleFunction<Object> g = (ToDoubleFunction<Object>) getter;
            ObjDoubleConsumer<Object> s = (ObjDoubleConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsDouble(src));
        });

        BOX_ACTIONS.put(Byte.class, NumericCopyActions::boxByte);
        BOX_ACTIONS.put(Short.class, NumericCopyActions::boxShort);
        BOX_ACTIONS.put(Integer.class, NumericCopyActions::boxInteger);
        BOX_ACTIONS.put(Long.class, NumericCopyActions::boxLong);
        BOX_ACTIONS.put(Float.class, NumericCopyActions::boxFloat);
        BOX_ACTIONS.put(Double.class, NumericCopyActions::boxDouble);
        BOX_ACTIONS.put(Boolean.class, (sourceType, getter, setter, fieldName) -> boxBoolean(sourceType, getter, setter));
        BOX_ACTIONS.put(Character.class, (sourceType, getter, setter, fieldName) -> boxChar(sourceType, getter, setter));
    }

    /**
     * 按 source/target 类型维度分派 fast path；未命中返回 null，由上层 fallback。
     */
    static FieldCopyAction create(Class<?> sourceType, Class<?> targetType,
                                  Object getter, Object setter, String fieldName) {
        if (sourceType.isPrimitive() && targetType.isPrimitive()) {
            return primitiveToPrimitive(sourceType, targetType, getter, setter, fieldName);
        }
        if (sourceType.isPrimitive()) {
            return primitiveToWrapper(sourceType, targetType, getter, setter, fieldName);
        }
        if (targetType.isPrimitive()) {
            return wrapperToPrimitive(sourceType, targetType, getter, setter, fieldName);
        }
        return wrapperToWrapper(sourceType, targetType, getter, setter, fieldName);
    }

    /**
     * 同类型 / 拓宽 / 窄化。
     */
    private static FieldCopyAction primitiveToPrimitive(Class<?> sourceType, Class<?> targetType,
                                                        Object getter, Object setter, String fieldName) {
        if (sourceType == targetType) {
            return samePrimitive(sourceType, getter, setter);
        }
        if (isWidening(sourceType, targetType)) {
            return widen(sourceType, targetType, getter, setter);
        }
        if (isNarrowing(sourceType, targetType)) {
            return narrow(sourceType, targetType, getter, setter, fieldName);
        }
        return null;
    }

    /**
     * 基本类型 → 包装类，如 int→Integer、char→Character。
     */
    private static FieldCopyAction primitiveToWrapper(Class<?> sourceType, Class<?> targetType,
                                                      Object getter, Object setter, String fieldName) {
        BoxActionFactory factory = BOX_ACTIONS.get(targetType);
        return factory == null ? null : factory.create(sourceType, getter, setter, fieldName);
    }

    /**
     * 包装类 → 基本类型，按 target 基本类型分派到 wrapperToXxx。
     */
    private static FieldCopyAction wrapperToPrimitive(Class<?> sourceType, Class<?> targetType,
                                                      Object getter, Object setter, String fieldName) {
        Function<Object, Object> objectGetter = typedGetter(getter, sourceType);
        if (objectGetter == null) {
            return null;
        }
        if (targetType == byte.class) {
            return wrapperToByte(sourceType, objectGetter, setter, fieldName);
        }
        if (targetType == short.class) {
            return wrapperToShort(sourceType, objectGetter, setter, fieldName);
        }
        if (targetType == int.class) {
            return wrapperToInt(sourceType, objectGetter, setter, fieldName);
        }
        if (targetType == long.class) {
            return wrapperToLong(sourceType, objectGetter, setter);
        }
        if (targetType == float.class) {
            return wrapperToFloat(sourceType, objectGetter, setter, fieldName);
        }
        if (targetType == double.class) {
            return wrapperToDouble(sourceType, objectGetter, setter);
        }
        if (targetType == boolean.class) {
            return wrapperToBoolean(sourceType, objectGetter, setter);
        }
        if (targetType == char.class) {
            return wrapperToChar(sourceType, objectGetter, setter);
        }
        return null;
    }

    private static FieldCopyAction wrapperToByte(Class<?> sourceType, Function<Object, Object> objectGetter,
                                                 Object setter, String fieldName) {
        ObjByteConsumer<Object> byteSetter = (ObjByteConsumer<Object>) setter;
        FieldCopyAction action = copyDirectWrapper(objectGetter, sourceType, Byte.class, byteSetter::accept);
        if (action != null) {
            return action;
        }
        if (Short.class.equals(sourceType)) {
            return copyWithShortToByte(objectGetter, byteSetter, fieldName);
        }
        if (Integer.class.equals(sourceType)) {
            return copyWithIntToByte(objectGetter, byteSetter, fieldName);
        }
        if (Long.class.equals(sourceType)) {
            return copyWithLongToByte(objectGetter, byteSetter, fieldName);
        }
        return null;
    }

    private static FieldCopyAction wrapperToShort(Class<?> sourceType, Function<Object, Object> objectGetter,
                                                  Object setter, String fieldName) {
        ObjShortConsumer<Object> shortSetter = (ObjShortConsumer<Object>) setter;
        if (Byte.class.equals(sourceType)) {
            return (src, tgt) -> {
                Byte value = (Byte) objectGetter.apply(src);
                if (value != null) {
                    shortSetter.accept(tgt, value);
                }
            };
        }
        FieldCopyAction action = copyDirectWrapper(objectGetter, sourceType, Short.class, shortSetter::accept);
        if (action != null) {
            return action;
        }
        if (Integer.class.equals(sourceType)) {
            return copyWithIntToShort(objectGetter, shortSetter, fieldName);
        }
        if (Long.class.equals(sourceType)) {
            return copyWithLongToShort(objectGetter, shortSetter, fieldName);
        }
        return null;
    }

    private static FieldCopyAction wrapperToInt(Class<?> sourceType, Function<Object, Object> objectGetter,
                                                Object setter, String fieldName) {
        ObjIntConsumer<Object> intSetter = (ObjIntConsumer<Object>) setter;
        if (Byte.class.equals(sourceType) || Short.class.equals(sourceType)) {
            return copyWrapperToInt(objectGetter, intSetter);
        }
        FieldCopyAction action = copyDirectWrapper(objectGetter, sourceType, Integer.class, intSetter::accept);
        if (action != null) {
            return action;
        }
        if (Long.class.equals(sourceType)) {
            return copyWithLongToInt(objectGetter, intSetter, fieldName);
        }
        return null;
    }

    private static FieldCopyAction wrapperToLong(Class<?> sourceType, Function<Object, Object> objectGetter,
                                                 Object setter) {
        if (!isIntegralWrapper(sourceType)) {
            return null;
        }
        ObjLongConsumer<Object> longSetter = (ObjLongConsumer<Object>) setter;
        return copyWrapperToLong(objectGetter, longSetter);
    }

    private static FieldCopyAction wrapperToFloat(Class<?> sourceType, Function<Object, Object> objectGetter,
                                                  Object setter, String fieldName) {
        ObjFloatConsumer<Object> floatSetter = (ObjFloatConsumer<Object>) setter;
        if (isIntegralWrapper(sourceType)) {
            return copyIntegralToFloat(objectGetter, floatSetter);
        }
        if (Double.class.equals(sourceType)) {
            return copyWithDoubleToFloat(objectGetter, floatSetter, fieldName);
        }
        return null;
    }

    private static FieldCopyAction wrapperToDouble(Class<?> sourceType, Function<Object, Object> objectGetter,
                                                   Object setter) {
        ObjDoubleConsumer<Object> doubleSetter = (ObjDoubleConsumer<Object>) setter;
        if (Float.class.equals(sourceType)) {
            return (src, tgt) -> {
                Float value = (Float) objectGetter.apply(src);
                if (value != null) {
                    doubleSetter.accept(tgt, value);
                }
            };
        }
        if (isIntegralWrapper(sourceType)) {
            return copyIntegralToDouble(objectGetter, doubleSetter);
        }
        return null;
    }

    private static FieldCopyAction wrapperToBoolean(Class<?> sourceType, Function<Object, Object> objectGetter,
                                                    Object setter) {
        if (!Boolean.class.equals(sourceType)) {
            return null;
        }
        ObjBooleanConsumer<Object> booleanSetter = (ObjBooleanConsumer<Object>) setter;
        return copyDirectWrapper(objectGetter, sourceType, Boolean.class, booleanSetter::accept);
    }

    private static FieldCopyAction wrapperToChar(Class<?> sourceType, Function<Object, Object> objectGetter,
                                                 Object setter) {
        if (!Character.class.equals(sourceType)) {
            return null;
        }
        ObjCharConsumer<Object> charSetter = (ObjCharConsumer<Object>) setter;
        return copyDirectWrapper(objectGetter, sourceType, Character.class, charSetter::accept);
    }

    /**
     * 包装类同名且非 null 时直传 setter。
     */
    private static <W> FieldCopyAction copyDirectWrapper(Function<Object, Object> getter, Class<?> sourceType,
                                                         Class<W> wrapperType, BiConsumer<Object, W> setter) {
        if (!wrapperType.equals(sourceType)) {
            return null;
        }
        return (src, tgt) -> {
            W value = wrapperType.cast(getter.apply(src));
            if (value != null) {
                setter.accept(tgt, value);
            }
        };
    }

    private static FieldCopyAction copyWrapperToInt(Function<Object, Object> getter,
                                                    ObjIntConsumer<Object> intSetter) {
        return (src, tgt) -> {
            Number value = (Number) getter.apply(src);
            if (value != null) {
                intSetter.accept(tgt, value.intValue());
            }
        };
    }

    private static FieldCopyAction copyWrapperToLong(Function<Object, Object> getter,
                                                     ObjLongConsumer<Object> longSetter) {
        return (src, tgt) -> {
            Number value = (Number) getter.apply(src);
            if (value != null) {
                longSetter.accept(tgt, value.longValue());
            }
        };
    }

    private static FieldCopyAction copyWithShortToByte(Function<Object, Object> getter,
                                                       ObjByteConsumer<Object> byteSetter, String fieldName) {
        return (src, tgt) -> {
            Short value = (Short) getter.apply(src);
            if (value != null) {
                byteSetter.accept(tgt, CopyValueConverter.byteFromShort(value, fieldName));
            }
        };
    }

    private static FieldCopyAction copyWithIntToByte(Function<Object, Object> getter,
                                                     ObjByteConsumer<Object> byteSetter, String fieldName) {
        return (src, tgt) -> {
            Integer value = (Integer) getter.apply(src);
            if (value != null) {
                byteSetter.accept(tgt, CopyValueConverter.byteFromInt(value, fieldName));
            }
        };
    }

    private static FieldCopyAction copyWithLongToByte(Function<Object, Object> getter,
                                                      ObjByteConsumer<Object> byteSetter, String fieldName) {
        return (src, tgt) -> {
            Long value = (Long) getter.apply(src);
            if (value != null) {
                byteSetter.accept(tgt, CopyValueConverter.byteFromLong(value, fieldName));
            }
        };
    }

    private static FieldCopyAction copyWithIntToShort(Function<Object, Object> getter,
                                                      ObjShortConsumer<Object> shortSetter, String fieldName) {
        return (src, tgt) -> {
            Integer value = (Integer) getter.apply(src);
            if (value != null) {
                shortSetter.accept(tgt, CopyValueConverter.shortFromInt(value, fieldName));
            }
        };
    }

    private static FieldCopyAction copyWithLongToShort(Function<Object, Object> getter,
                                                       ObjShortConsumer<Object> shortSetter, String fieldName) {
        return (src, tgt) -> {
            Long value = (Long) getter.apply(src);
            if (value != null) {
                shortSetter.accept(tgt, CopyValueConverter.shortFromLong(value, fieldName));
            }
        };
    }

    private static FieldCopyAction copyWithLongToInt(Function<Object, Object> getter,
                                                     ObjIntConsumer<Object> intSetter, String fieldName) {
        return (src, tgt) -> {
            Long value = (Long) getter.apply(src);
            if (value != null) {
                intSetter.accept(tgt, CopyValueConverter.intFromLong(value, fieldName));
            }
        };
    }

    private static FieldCopyAction copyIntegralToFloat(Function<Object, Object> getter,
                                                       ObjFloatConsumer<Object> floatSetter) {
        return (src, tgt) -> {
            Number value = (Number) getter.apply(src);
            if (value != null) {
                floatSetter.accept(tgt, value.longValue());
            }
        };
    }

    private static FieldCopyAction copyWithDoubleToFloat(Function<Object, Object> getter,
                                                         ObjFloatConsumer<Object> floatSetter, String fieldName) {
        return (src, tgt) -> {
            Double value = (Double) getter.apply(src);
            if (value != null) {
                floatSetter.accept(tgt, CopyValueConverter.floatFromDouble(value, fieldName));
            }
        };
    }

    private static FieldCopyAction copyIntegralToDouble(Function<Object, Object> getter,
                                                        ObjDoubleConsumer<Object> doubleSetter) {
        return (src, tgt) -> {
            Number value = (Number) getter.apply(src);
            if (value != null) {
                doubleSetter.accept(tgt, value.longValue());
            }
        };
    }

    /**
     * 包装类同类型直传，或整型/浮点包装之间的拓宽窄化。
     */
    private static FieldCopyAction wrapperToWrapper(Class<?> sourceType, Class<?> targetType,
                                                    Object getter, Object setter, String fieldName) {
        Function<Object, Object> objectGetter = typedGetter(getter, sourceType);
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        if (objectGetter == null || objectSetter == null) {
            return null;
        }
        if (sourceType.equals(targetType)) {
            return copySameWrapper(objectGetter, objectSetter);
        }
        FieldCopyAction special = specialWrapperPair(sourceType, targetType, objectGetter, objectSetter, fieldName);
        if (special != null) {
            return special;
        }
        if (isIntegralWrapper(sourceType) && isIntegralWrapper(targetType)) {
            return copyIntegralWrapperPair(objectGetter, objectSetter, sourceType, targetType, fieldName);
        }
        if (isIntegralWrapper(sourceType) && isFloatingWrapper(targetType)) {
            return copyIntegralToFloatingWrapper(objectGetter, objectSetter, targetType);
        }
        if (Float.class.equals(sourceType) && Double.class.equals(targetType)) {
            return copyFloatToDoubleWrapper(objectGetter, objectSetter);
        }
        return null;
    }

    private static FieldCopyAction copySameWrapper(Function<Object, Object> objectGetter,
                                                   BiConsumer<Object, Object> objectSetter) {
        return (src, tgt) -> {
            Object value = objectGetter.apply(src);
            if (value != null) {
                objectSetter.accept(tgt, value);
            }
        };
    }

    private static FieldCopyAction specialWrapperPair(Class<?> sourceType, Class<?> targetType,
                                                      Function<Object, Object> objectGetter,
                                                      BiConsumer<Object, Object> objectSetter, String fieldName) {
        if (Byte.class.equals(sourceType) && Long.class.equals(targetType)) {
            return copyByteToLongWrapper(objectGetter, objectSetter);
        }
        if (Short.class.equals(sourceType) && Double.class.equals(targetType)) {
            return copyShortToDoubleWrapper(objectGetter, objectSetter);
        }
        if (Integer.class.equals(sourceType) && Double.class.equals(targetType)) {
            return copyIntegerToDoubleWrapper(objectGetter, objectSetter);
        }
        if (Integer.class.equals(sourceType) && Long.class.equals(targetType)) {
            return copyIntegerToLongWrapper(objectGetter, objectSetter);
        }
        if (Long.class.equals(sourceType) && Float.class.equals(targetType)) {
            return copyLongToFloatWrapper(objectGetter, objectSetter);
        }
        if (Double.class.equals(sourceType) && Float.class.equals(targetType)) {
            return copyDoubleToFloatWrapper(objectGetter, objectSetter, fieldName);
        }
        return null;
    }

    private static FieldCopyAction copyByteToLongWrapper(Function<Object, Object> objectGetter,
                                                         BiConsumer<Object, Object> objectSetter) {
        return (src, tgt) -> {
            Byte value = (Byte) objectGetter.apply(src);
            if (value != null) {
                objectSetter.accept(tgt, value.longValue());
            }
        };
    }

    private static FieldCopyAction copyShortToDoubleWrapper(Function<Object, Object> objectGetter,
                                                            BiConsumer<Object, Object> objectSetter) {
        return (src, tgt) -> {
            Short value = (Short) objectGetter.apply(src);
            if (value != null) {
                objectSetter.accept(tgt, (double) value);
            }
        };
    }

    private static FieldCopyAction copyIntegerToDoubleWrapper(Function<Object, Object> objectGetter,
                                                              BiConsumer<Object, Object> objectSetter) {
        return (src, tgt) -> {
            Integer value = (Integer) objectGetter.apply(src);
            if (value != null) {
                objectSetter.accept(tgt, (double) value);
            }
        };
    }

    private static FieldCopyAction copyIntegerToLongWrapper(Function<Object, Object> objectGetter,
                                                            BiConsumer<Object, Object> objectSetter) {
        return (src, tgt) -> {
            Integer value = (Integer) objectGetter.apply(src);
            if (value != null) {
                objectSetter.accept(tgt, value.longValue());
            }
        };
    }

    private static FieldCopyAction copyLongToFloatWrapper(Function<Object, Object> objectGetter,
                                                          BiConsumer<Object, Object> objectSetter) {
        return (src, tgt) -> {
            Long value = (Long) objectGetter.apply(src);
            if (value != null) {
                objectSetter.accept(tgt, (float) (long) value);
            }
        };
    }

    private static FieldCopyAction copyDoubleToFloatWrapper(Function<Object, Object> objectGetter,
                                                            BiConsumer<Object, Object> objectSetter,
                                                            String fieldName) {
        return (src, tgt) -> {
            Double value = (Double) objectGetter.apply(src);
            if (value != null) {
                objectSetter.accept(tgt, CopyValueConverter.floatFromDouble(value, fieldName));
            }
        };
    }

    private static FieldCopyAction copyIntegralWrapperPair(Function<Object, Object> objectGetter,
                                                           BiConsumer<Object, Object> objectSetter,
                                                           Class<?> sourceType, Class<?> targetType,
                                                           String fieldName) {
        return (src, tgt) -> {
            Number value = (Number) objectGetter.apply(src);
            if (value == null) {
                return;
            }
            objectSetter.accept(tgt, convertIntegralWrapper(value, sourceType, targetType, fieldName));
        };
    }

    private static FieldCopyAction copyIntegralToFloatingWrapper(Function<Object, Object> objectGetter,
                                                                 BiConsumer<Object, Object> objectSetter,
                                                                 Class<?> targetType) {
        return (src, tgt) -> {
            Number value = (Number) objectGetter.apply(src);
            if (value == null) {
                return;
            }
            if (Float.class.equals(targetType)) {
                objectSetter.accept(tgt, (float) value.longValue());
            } else {
                objectSetter.accept(tgt, (double) value.longValue());
            }
        };
    }

    private static FieldCopyAction copyFloatToDoubleWrapper(Function<Object, Object> objectGetter,
                                                            BiConsumer<Object, Object> objectSetter) {
        return (src, tgt) -> {
            Float value = (Float) objectGetter.apply(src);
            if (value != null) {
                objectSetter.accept(tgt, (double) value);
            }
        };
    }

    private static Function<Object, Object> typedGetter(Object getter, Class<?> sourceType) {
        if (getter instanceof Function) {
            return (Function<Object, Object>) getter;
        }
        return (Function<Object, Object>) GetSetHelper.asFunctionGetter(getter);
    }

    private static boolean isIntegralWrapper(Class<?> type) {
        return Byte.class.equals(type) || Short.class.equals(type)
                || Integer.class.equals(type) || Long.class.equals(type);
    }

    private static boolean isFloatingWrapper(Class<?> type) {
        return Float.class.equals(type) || Double.class.equals(type);
    }

    private static Object convertIntegralWrapper(Number value, Class<?> sourceType, Class<?> targetType,
                                                 String fieldName) {
        long longValue = value.longValue();
        if (Long.class.equals(targetType)) {
            return longValue;
        }
        if (Integer.class.equals(targetType)) {
            return CopyValueConverter.intFromLong(longValue, fieldName);
        }
        if (Short.class.equals(targetType)) {
            return CopyValueConverter.shortFromLong(longValue, fieldName);
        }
        if (Byte.class.equals(targetType)) {
            return CopyValueConverter.byteFromLong(longValue, fieldName);
        }
        return CopyValueConverter.convertNumber(value, targetType, fieldName);
    }

    private static FieldCopyAction boxByte(Class<?> sourceType, Object getter, Object setter, String fieldName) {
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        if (sourceType == byte.class) {
            ToByteFunction<Object> byteGetter = (ToByteFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, byteGetter.applyAsByte(src));
        }
        if (sourceType == short.class) {
            ToShortFunction<Object> shortGetter = (ToShortFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt,
                    CopyValueConverter.byteFromShort(shortGetter.applyAsShort(src), fieldName));
        }
        if (sourceType == int.class) {
            ToIntFunction<Object> intGetter = (ToIntFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt,
                    CopyValueConverter.byteFromInt(intGetter.applyAsInt(src), fieldName));
        }
        if (sourceType == long.class) {
            ToLongFunction<Object> longGetter = (ToLongFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt,
                    CopyValueConverter.byteFromLong(longGetter.applyAsLong(src), fieldName));
        }
        return null;
    }

    private static FieldCopyAction boxShort(Class<?> sourceType, Object getter, Object setter, String fieldName) {
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        if (sourceType == byte.class) {
            ToByteFunction<Object> byteGetter = (ToByteFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, byteGetter.applyAsByte(src));
        }
        if (sourceType == short.class) {
            ToShortFunction<Object> shortGetter = (ToShortFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, shortGetter.applyAsShort(src));
        }
        if (sourceType == int.class) {
            ToIntFunction<Object> intGetter = (ToIntFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt,
                    CopyValueConverter.shortFromInt(intGetter.applyAsInt(src), fieldName));
        }
        if (sourceType == long.class) {
            ToLongFunction<Object> longGetter = (ToLongFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt,
                    CopyValueConverter.shortFromLong(longGetter.applyAsLong(src), fieldName));
        }
        return null;
    }

    private static FieldCopyAction boxInteger(Class<?> sourceType, Object getter, Object setter, String fieldName) {
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        if (sourceType == byte.class) {
            ToByteFunction<Object> byteGetter = (ToByteFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, byteGetter.applyAsByte(src));
        }
        if (sourceType == short.class) {
            ToShortFunction<Object> shortGetter = (ToShortFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, shortGetter.applyAsShort(src));
        }
        if (sourceType == int.class) {
            ToIntFunction<Object> intGetter = (ToIntFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, intGetter.applyAsInt(src));
        }
        if (sourceType == long.class) {
            ToLongFunction<Object> longGetter = (ToLongFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt,
                    CopyValueConverter.intFromLong(longGetter.applyAsLong(src), fieldName));
        }
        return null;
    }

    private static FieldCopyAction boxLong(Class<?> sourceType, Object getter, Object setter, String fieldName) {
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        if (sourceType == byte.class) {
            ToByteFunction<Object> byteGetter = (ToByteFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, (long) byteGetter.applyAsByte(src));
        }
        if (sourceType == short.class) {
            ToShortFunction<Object> shortGetter = (ToShortFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, (long) shortGetter.applyAsShort(src));
        }
        if (sourceType == int.class) {
            ToIntFunction<Object> intGetter = (ToIntFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, (long) intGetter.applyAsInt(src));
        }
        if (sourceType == long.class) {
            ToLongFunction<Object> longGetter = (ToLongFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, longGetter.applyAsLong(src));
        }
        return null;
    }

    private static FieldCopyAction boxFloat(Class<?> sourceType, Object getter, Object setter, String fieldName) {
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        if (sourceType == byte.class) {
            ToByteFunction<Object> byteGetter = (ToByteFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, (float) byteGetter.applyAsByte(src));
        }
        if (sourceType == short.class) {
            ToShortFunction<Object> shortGetter = (ToShortFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, (float) shortGetter.applyAsShort(src));
        }
        if (sourceType == int.class) {
            ToIntFunction<Object> intGetter = (ToIntFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, (float) intGetter.applyAsInt(src));
        }
        if (sourceType == long.class) {
            ToLongFunction<Object> longGetter = (ToLongFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, (float) longGetter.applyAsLong(src));
        }
        if (sourceType == float.class) {
            ToFloatFunction<Object> floatGetter = (ToFloatFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, floatGetter.applyAsFloat(src));
        }
        if (sourceType == double.class) {
            ToDoubleFunction<Object> doubleGetter = (ToDoubleFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt,
                    CopyValueConverter.floatFromDouble(doubleGetter.applyAsDouble(src), fieldName));
        }
        return null;
    }

    private static FieldCopyAction boxDouble(Class<?> sourceType, Object getter, Object setter, String fieldName) {
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        if (sourceType == float.class) {
            ToFloatFunction<Object> floatGetter = (ToFloatFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, (double) floatGetter.applyAsFloat(src));
        }
        if (sourceType == byte.class) {
            ToByteFunction<Object> byteGetter = (ToByteFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, (double) byteGetter.applyAsByte(src));
        }
        if (sourceType == short.class) {
            ToShortFunction<Object> shortGetter = (ToShortFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, (double) shortGetter.applyAsShort(src));
        }
        if (sourceType == int.class) {
            ToIntFunction<Object> intGetter = (ToIntFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, (double) intGetter.applyAsInt(src));
        }
        if (sourceType == long.class) {
            ToLongFunction<Object> longGetter = (ToLongFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, (double) longGetter.applyAsLong(src));
        }
        if (sourceType == double.class) {
            ToDoubleFunction<Object> doubleGetter = (ToDoubleFunction<Object>) getter;
            return (src, tgt) -> objectSetter.accept(tgt, doubleGetter.applyAsDouble(src));
        }
        return null;
    }

    private static FieldCopyAction boxBoolean(Class<?> sourceType, Object getter, Object setter) {
        if (sourceType != boolean.class) {
            return null;
        }
        ToBooleanFunction<Object> booleanGetter = (ToBooleanFunction<Object>) getter;
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        return (src, tgt) -> objectSetter.accept(tgt, booleanGetter.applyAsBoolean(src));
    }

    private static FieldCopyAction boxChar(Class<?> sourceType, Object getter, Object setter) {
        if (sourceType != char.class) {
            return null;
        }
        ToCharFunction<Object> charGetter = (ToCharFunction<Object>) getter;
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        return (src, tgt) -> objectSetter.accept(tgt, charGetter.applyAsChar(src));
    }

    private static boolean isWidening(Class<?> source, Class<?> target) {
        return level(source) < level(target);
    }

    private static boolean isNarrowing(Class<?> source, Class<?> target) {
        return level(source) > level(target);
    }

    private static int level(Class<?> primitive) {
        if (primitive == byte.class) {
            return 0;
        }
        if (primitive == short.class) {
            return 1;
        }
        if (primitive == int.class) {
            return 2;
        }
        if (primitive == long.class) {
            return 3;
        }
        if (primitive == float.class) {
            return 4;
        }
        if (primitive == double.class) {
            return 5;
        }
        return -1;
    }

    /**
     * 同基本类型：To*Function → Obj*Consumer，全程无装箱。
     */
    private static FieldCopyAction samePrimitive(Class<?> type, Object getter, Object setter) {
        SamePrimitiveFactory factory = SAME_PRIMITIVE.get(type);
        return factory == null ? null : factory.create(getter, setter);
    }

    /**
     * 基本类型拓宽（byte→int 等），按源类型分派。
     */
    private static FieldCopyAction widen(Class<?> sourceType, Class<?> targetType, Object getter, Object setter) {
        if (sourceType == byte.class) {
            return widenFromByte((ToByteFunction<Object>) getter, targetType, setter);
        }
        if (sourceType == short.class) {
            return widenFromShort((ToShortFunction<Object>) getter, targetType, setter);
        }
        if (sourceType == int.class) {
            return widenFromInt((ToIntFunction<Object>) getter, targetType, setter);
        }
        if (sourceType == long.class) {
            return widenFromLong((ToLongFunction<Object>) getter, targetType, setter);
        }
        if (sourceType == float.class && targetType == double.class) {
            ToFloatFunction<Object> g = (ToFloatFunction<Object>) getter;
            ObjDoubleConsumer<Object> s = (ObjDoubleConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsFloat(src));
        }
        return null;
    }

    private static FieldCopyAction widenFromByte(ToByteFunction<Object> getter, Class<?> targetType, Object setter) {
        if (targetType == short.class) {
            return (src, tgt) -> ((ObjShortConsumer<Object>) setter).accept(tgt, getter.applyAsByte(src));
        }
        if (targetType == int.class) {
            return (src, tgt) -> ((ObjIntConsumer<Object>) setter).accept(tgt, getter.applyAsByte(src));
        }
        if (targetType == long.class) {
            return (src, tgt) -> ((ObjLongConsumer<Object>) setter).accept(tgt, getter.applyAsByte(src));
        }
        if (targetType == float.class) {
            return (src, tgt) -> ((ObjFloatConsumer<Object>) setter).accept(tgt, getter.applyAsByte(src));
        }
        if (targetType == double.class) {
            return (src, tgt) -> ((ObjDoubleConsumer<Object>) setter).accept(tgt, getter.applyAsByte(src));
        }
        return null;
    }

    private static FieldCopyAction widenFromShort(ToShortFunction<Object> getter, Class<?> targetType, Object setter) {
        if (targetType == int.class) {
            return (src, tgt) -> ((ObjIntConsumer<Object>) setter).accept(tgt, getter.applyAsShort(src));
        }
        if (targetType == long.class) {
            return (src, tgt) -> ((ObjLongConsumer<Object>) setter).accept(tgt, getter.applyAsShort(src));
        }
        if (targetType == float.class) {
            return (src, tgt) -> ((ObjFloatConsumer<Object>) setter).accept(tgt, getter.applyAsShort(src));
        }
        if (targetType == double.class) {
            return (src, tgt) -> ((ObjDoubleConsumer<Object>) setter).accept(tgt, getter.applyAsShort(src));
        }
        return null;
    }

    private static FieldCopyAction widenFromInt(ToIntFunction<Object> getter, Class<?> targetType, Object setter) {
        if (targetType == long.class) {
            return (src, tgt) -> ((ObjLongConsumer<Object>) setter).accept(tgt, getter.applyAsInt(src));
        }
        if (targetType == float.class) {
            return (src, tgt) -> ((ObjFloatConsumer<Object>) setter).accept(tgt, getter.applyAsInt(src));
        }
        if (targetType == double.class) {
            return (src, tgt) -> ((ObjDoubleConsumer<Object>) setter).accept(tgt, getter.applyAsInt(src));
        }
        return null;
    }

    private static FieldCopyAction widenFromLong(ToLongFunction<Object> getter, Class<?> targetType, Object setter) {
        if (targetType == float.class) {
            return (src, tgt) -> ((ObjFloatConsumer<Object>) setter).accept(tgt, getter.applyAsLong(src));
        }
        if (targetType == double.class) {
            return (src, tgt) -> ((ObjDoubleConsumer<Object>) setter).accept(tgt, getter.applyAsLong(src));
        }
        return null;
    }

    /**
     * 基本类型窄化，溢出时 fieldName 写入 {@link CopyException}。
     */
    private static FieldCopyAction narrow(Class<?> sourceType, Class<?> targetType,
                                          Object getter, Object setter, String fieldName) {
        if (sourceType == long.class) {
            return narrowFromLong((ToLongFunction<Object>) getter, targetType, setter, fieldName);
        }
        if (sourceType == int.class) {
            return narrowFromInt((ToIntFunction<Object>) getter, targetType, setter, fieldName);
        }
        if (sourceType == short.class && targetType == byte.class) {
            ToShortFunction<Object> g = (ToShortFunction<Object>) getter;
            ObjByteConsumer<Object> s = (ObjByteConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, CopyValueConverter.byteFromShort(g.applyAsShort(src), fieldName));
        }
        if (sourceType == double.class && targetType == float.class) {
            ToDoubleFunction<Object> g = (ToDoubleFunction<Object>) getter;
            ObjFloatConsumer<Object> s = (ObjFloatConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, CopyValueConverter.floatFromDouble(g.applyAsDouble(src), fieldName));
        }
        return null;
    }

    private static FieldCopyAction narrowFromLong(ToLongFunction<Object> getter, Class<?> targetType,
                                                  Object setter, String fieldName) {
        if (targetType == int.class) {
            return (src, tgt) -> ((ObjIntConsumer<Object>) setter).accept(tgt,
                    CopyValueConverter.intFromLong(getter.applyAsLong(src), fieldName));
        }
        if (targetType == short.class) {
            return (src, tgt) -> ((ObjShortConsumer<Object>) setter).accept(tgt,
                    CopyValueConverter.shortFromLong(getter.applyAsLong(src), fieldName));
        }
        if (targetType == byte.class) {
            return (src, tgt) -> ((ObjByteConsumer<Object>) setter).accept(tgt,
                    CopyValueConverter.byteFromLong(getter.applyAsLong(src), fieldName));
        }
        return null;
    }

    private static FieldCopyAction narrowFromInt(ToIntFunction<Object> getter, Class<?> targetType,
                                                 Object setter, String fieldName) {
        if (targetType == short.class) {
            return (src, tgt) -> ((ObjShortConsumer<Object>) setter).accept(tgt,
                    CopyValueConverter.shortFromInt(getter.applyAsInt(src), fieldName));
        }
        if (targetType == byte.class) {
            return (src, tgt) -> ((ObjByteConsumer<Object>) setter).accept(tgt,
                    CopyValueConverter.byteFromInt(getter.applyAsInt(src), fieldName));
        }
        return null;
    }
}
