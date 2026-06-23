package com.taowater.taol.core.convert;

import com.taowater.taol.core.function.*;

import java.util.function.*;

/**
 * 标量数值拷贝 fast path：基本类型 / 包装类型之间的拓宽、窄化，避免运行时 convert 分发。
 */
final class NumericCopyActions {

    private NumericCopyActions() {
    }

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

    private static FieldCopyAction primitiveToWrapper(Class<?> sourceType, Class<?> targetType,
                                                      Object getter, Object setter, String fieldName) {
        if (Byte.class.equals(targetType)) {
            return boxByte(sourceType, getter, setter, fieldName);
        }
        if (Short.class.equals(targetType)) {
            return boxShort(sourceType, getter, setter, fieldName);
        }
        if (Integer.class.equals(targetType)) {
            return boxInteger(sourceType, getter, setter, fieldName);
        }
        if (Long.class.equals(targetType)) {
            return boxLong(sourceType, getter, setter, fieldName);
        }
        if (Float.class.equals(targetType)) {
            return boxFloat(sourceType, getter, setter, fieldName);
        }
        if (Double.class.equals(targetType)) {
            return boxDouble(sourceType, getter, setter, fieldName);
        }
        return null;
    }

    private static FieldCopyAction wrapperToPrimitive(Class<?> sourceType, Class<?> targetType,
                                                      Object getter, Object setter, String fieldName) {
        Function<Object, Object> objectGetter = typedGetter(getter, sourceType);
        if (objectGetter == null) {
            return null;
        }
        if (targetType == byte.class) {
            ObjByteConsumer<Object> byteSetter = (ObjByteConsumer<Object>) setter;
            if (Byte.class.equals(sourceType)) {
                return (src, tgt) -> {
                    Byte value = (Byte) objectGetter.apply(src);
                    if (value != null) {
                        byteSetter.accept(tgt, value);
                    }
                };
            }
            if (Short.class.equals(sourceType)) {
                return (src, tgt) -> {
                    Short value = (Short) objectGetter.apply(src);
                    if (value != null) {
                        byteSetter.accept(tgt, CopyValueConverter.byteFromShort(value, fieldName));
                    }
                };
            }
            if (Integer.class.equals(sourceType)) {
                return (src, tgt) -> {
                    Integer value = (Integer) objectGetter.apply(src);
                    if (value != null) {
                        byteSetter.accept(tgt, CopyValueConverter.byteFromInt(value, fieldName));
                    }
                };
            }
            if (Long.class.equals(sourceType)) {
                return (src, tgt) -> {
                    Long value = (Long) objectGetter.apply(src);
                    if (value != null) {
                        byteSetter.accept(tgt, CopyValueConverter.byteFromLong(value, fieldName));
                    }
                };
            }
        }
        if (targetType == short.class) {
            ObjShortConsumer<Object> shortSetter = (ObjShortConsumer<Object>) setter;
            if (Byte.class.equals(sourceType)) {
                return (src, tgt) -> {
                    Byte value = (Byte) objectGetter.apply(src);
                    if (value != null) {
                        shortSetter.accept(tgt, value);
                    }
                };
            }
            if (Short.class.equals(sourceType)) {
                return (src, tgt) -> {
                    Short value = (Short) objectGetter.apply(src);
                    if (value != null) {
                        shortSetter.accept(tgt, value);
                    }
                };
            }
            if (Integer.class.equals(sourceType)) {
                return (src, tgt) -> {
                    Integer value = (Integer) objectGetter.apply(src);
                    if (value != null) {
                        shortSetter.accept(tgt, CopyValueConverter.shortFromInt(value, fieldName));
                    }
                };
            }
            if (Long.class.equals(sourceType)) {
                return (src, tgt) -> {
                    Long value = (Long) objectGetter.apply(src);
                    if (value != null) {
                        shortSetter.accept(tgt, CopyValueConverter.shortFromLong(value, fieldName));
                    }
                };
            }
        }
        if (targetType == int.class) {
            ObjIntConsumer<Object> intSetter = (ObjIntConsumer<Object>) setter;
            if (Byte.class.equals(sourceType) || Short.class.equals(sourceType)) {
                return (src, tgt) -> {
                    Number value = (Number) objectGetter.apply(src);
                    if (value != null) {
                        intSetter.accept(tgt, value.intValue());
                    }
                };
            }
            if (Integer.class.equals(sourceType)) {
                return (src, tgt) -> {
                    Integer value = (Integer) objectGetter.apply(src);
                    if (value != null) {
                        intSetter.accept(tgt, value);
                    }
                };
            }
            if (Long.class.equals(sourceType)) {
                return (src, tgt) -> {
                    Long value = (Long) objectGetter.apply(src);
                    if (value != null) {
                        intSetter.accept(tgt, CopyValueConverter.intFromLong(value, fieldName));
                    }
                };
            }
        }
        if (targetType == long.class) {
            ObjLongConsumer<Object> longSetter = (ObjLongConsumer<Object>) setter;
            if (isIntegralWrapper(sourceType)) {
                return (src, tgt) -> {
                    Number value = (Number) objectGetter.apply(src);
                    if (value != null) {
                        longSetter.accept(tgt, value.longValue());
                    }
                };
            }
        }
        if (targetType == float.class) {
            ObjFloatConsumer<Object> floatSetter = (ObjFloatConsumer<Object>) setter;
            if (isIntegralWrapper(sourceType)) {
                return (src, tgt) -> {
                    Number value = (Number) objectGetter.apply(src);
                    if (value != null) {
                        floatSetter.accept(tgt, value.longValue());
                    }
                };
            }
            if (Double.class.equals(sourceType)) {
                return (src, tgt) -> {
                    Double value = (Double) objectGetter.apply(src);
                    if (value != null) {
                        floatSetter.accept(tgt, CopyValueConverter.floatFromDouble(value, fieldName));
                    }
                };
            }
        }
        if (targetType == double.class) {
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
                return (src, tgt) -> {
                    Number value = (Number) objectGetter.apply(src);
                    if (value != null) {
                        doubleSetter.accept(tgt, value.longValue());
                    }
                };
            }
        }
        return null;
    }

    private static FieldCopyAction wrapperToWrapper(Class<?> sourceType, Class<?> targetType,
                                                    Object getter, Object setter, String fieldName) {
        Function<Object, Object> objectGetter = typedGetter(getter, sourceType);
        BiConsumer<Object, Object> objectSetter = (BiConsumer<Object, Object>) GetSetHelper.asBiConsumerSetter(setter);
        if (objectGetter == null || objectSetter == null) {
            return null;
        }
        if (sourceType.equals(targetType)) {
            return (src, tgt) -> {
                Object value = objectGetter.apply(src);
                if (value != null) {
                    objectSetter.accept(tgt, value);
                }
            };
        }
        if (Byte.class.equals(sourceType) && Long.class.equals(targetType)) {
            return (src, tgt) -> {
                Byte value = (Byte) objectGetter.apply(src);
                if (value != null) {
                    objectSetter.accept(tgt, value.longValue());
                }
            };
        }
        if (Short.class.equals(sourceType) && Double.class.equals(targetType)) {
            return (src, tgt) -> {
                Short value = (Short) objectGetter.apply(src);
                if (value != null) {
                    objectSetter.accept(tgt, (double) value);
                }
            };
        }
        if (Integer.class.equals(sourceType) && Double.class.equals(targetType)) {
            return (src, tgt) -> {
                Integer value = (Integer) objectGetter.apply(src);
                if (value != null) {
                    objectSetter.accept(tgt, (double) value);
                }
            };
        }
        if (Integer.class.equals(sourceType) && Long.class.equals(targetType)) {
            return (src, tgt) -> {
                Integer value = (Integer) objectGetter.apply(src);
                if (value != null) {
                    objectSetter.accept(tgt, value.longValue());
                }
            };
        }
        if (Long.class.equals(sourceType) && Float.class.equals(targetType)) {
            return (src, tgt) -> {
                Long value = (Long) objectGetter.apply(src);
                if (value != null) {
                    objectSetter.accept(tgt, (float) (long) value);
                }
            };
        }
        if (Double.class.equals(sourceType) && Float.class.equals(targetType)) {
            return (src, tgt) -> {
                Double value = (Double) objectGetter.apply(src);
                if (value != null) {
                    objectSetter.accept(tgt, CopyValueConverter.floatFromDouble(value, fieldName));
                }
            };
        }
        if (isIntegralWrapper(sourceType) && isIntegralWrapper(targetType)) {
            return (src, tgt) -> {
                Number value = (Number) objectGetter.apply(src);
                if (value == null) {
                    return;
                }
                objectSetter.accept(tgt, convertIntegralWrapper(value, sourceType, targetType, fieldName));
            };
        }
        if (isIntegralWrapper(sourceType) && isFloatingWrapper(targetType)) {
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
        if (Float.class.equals(sourceType) && Double.class.equals(targetType)) {
            return (src, tgt) -> {
                Float value = (Float) objectGetter.apply(src);
                if (value != null) {
                    objectSetter.accept(tgt, (double) value);
                }
            };
        }
        return null;
    }

    @SuppressWarnings("unchecked")
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

    private static FieldCopyAction samePrimitive(Class<?> type, Object getter, Object setter) {
        if (type == boolean.class) {
            ToBooleanFunction<Object> g = (ToBooleanFunction<Object>) getter;
            ObjBooleanConsumer<Object> s = (ObjBooleanConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsBoolean(src));
        }
        if (type == byte.class) {
            ToByteFunction<Object> g = (ToByteFunction<Object>) getter;
            ObjByteConsumer<Object> s = (ObjByteConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsByte(src));
        }
        if (type == short.class) {
            ToShortFunction<Object> g = (ToShortFunction<Object>) getter;
            ObjShortConsumer<Object> s = (ObjShortConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsShort(src));
        }
        if (type == int.class) {
            ToIntFunction<Object> g = (ToIntFunction<Object>) getter;
            ObjIntConsumer<Object> s = (ObjIntConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsInt(src));
        }
        if (type == long.class) {
            ToLongFunction<Object> g = (ToLongFunction<Object>) getter;
            ObjLongConsumer<Object> s = (ObjLongConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsLong(src));
        }
        if (type == float.class) {
            ToFloatFunction<Object> g = (ToFloatFunction<Object>) getter;
            ObjFloatConsumer<Object> s = (ObjFloatConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsFloat(src));
        }
        if (type == double.class) {
            ToDoubleFunction<Object> g = (ToDoubleFunction<Object>) getter;
            ObjDoubleConsumer<Object> s = (ObjDoubleConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsDouble(src));
        }
        return null;
    }

    private static FieldCopyAction widen(Class<?> sourceType, Class<?> targetType, Object getter, Object setter) {
        if (sourceType == byte.class) {
            ToByteFunction<Object> g = (ToByteFunction<Object>) getter;
            if (targetType == short.class) {
                ObjShortConsumer<Object> s = (ObjShortConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsByte(src));
            }
            if (targetType == int.class) {
                ObjIntConsumer<Object> s = (ObjIntConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsByte(src));
            }
            if (targetType == long.class) {
                ObjLongConsumer<Object> s = (ObjLongConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsByte(src));
            }
            if (targetType == float.class) {
                ObjFloatConsumer<Object> s = (ObjFloatConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsByte(src));
            }
            if (targetType == double.class) {
                ObjDoubleConsumer<Object> s = (ObjDoubleConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsByte(src));
            }
        }
        if (sourceType == short.class) {
            ToShortFunction<Object> g = (ToShortFunction<Object>) getter;
            if (targetType == int.class) {
                ObjIntConsumer<Object> s = (ObjIntConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsShort(src));
            }
            if (targetType == long.class) {
                ObjLongConsumer<Object> s = (ObjLongConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsShort(src));
            }
            if (targetType == float.class) {
                ObjFloatConsumer<Object> s = (ObjFloatConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsShort(src));
            }
            if (targetType == double.class) {
                ObjDoubleConsumer<Object> s = (ObjDoubleConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsShort(src));
            }
        }
        if (sourceType == int.class) {
            ToIntFunction<Object> g = (ToIntFunction<Object>) getter;
            if (targetType == long.class) {
                ObjLongConsumer<Object> s = (ObjLongConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsInt(src));
            }
            if (targetType == float.class) {
                ObjFloatConsumer<Object> s = (ObjFloatConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsInt(src));
            }
            if (targetType == double.class) {
                ObjDoubleConsumer<Object> s = (ObjDoubleConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsInt(src));
            }
        }
        if (sourceType == long.class) {
            ToLongFunction<Object> g = (ToLongFunction<Object>) getter;
            if (targetType == float.class) {
                ObjFloatConsumer<Object> s = (ObjFloatConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsLong(src));
            }
            if (targetType == double.class) {
                ObjDoubleConsumer<Object> s = (ObjDoubleConsumer<Object>) setter;
                return (src, tgt) -> s.accept(tgt, g.applyAsLong(src));
            }
        }
        if (sourceType == float.class && targetType == double.class) {
            ToFloatFunction<Object> g = (ToFloatFunction<Object>) getter;
            ObjDoubleConsumer<Object> s = (ObjDoubleConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, g.applyAsFloat(src));
        }
        return null;
    }

    private static FieldCopyAction narrow(Class<?> sourceType, Class<?> targetType,
                                          Object getter, Object setter, String fieldName) {
        if (sourceType == long.class && targetType == int.class) {
            ToLongFunction<Object> g = (ToLongFunction<Object>) getter;
            ObjIntConsumer<Object> s = (ObjIntConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, CopyValueConverter.intFromLong(g.applyAsLong(src), fieldName));
        }
        if (sourceType == long.class && targetType == short.class) {
            ToLongFunction<Object> g = (ToLongFunction<Object>) getter;
            ObjShortConsumer<Object> s = (ObjShortConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, CopyValueConverter.shortFromLong(g.applyAsLong(src), fieldName));
        }
        if (sourceType == long.class && targetType == byte.class) {
            ToLongFunction<Object> g = (ToLongFunction<Object>) getter;
            ObjByteConsumer<Object> s = (ObjByteConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, CopyValueConverter.byteFromLong(g.applyAsLong(src), fieldName));
        }
        if (sourceType == int.class && targetType == short.class) {
            ToIntFunction<Object> g = (ToIntFunction<Object>) getter;
            ObjShortConsumer<Object> s = (ObjShortConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, CopyValueConverter.shortFromInt(g.applyAsInt(src), fieldName));
        }
        if (sourceType == int.class && targetType == byte.class) {
            ToIntFunction<Object> g = (ToIntFunction<Object>) getter;
            ObjByteConsumer<Object> s = (ObjByteConsumer<Object>) setter;
            return (src, tgt) -> s.accept(tgt, CopyValueConverter.byteFromInt(g.applyAsInt(src), fieldName));
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
}
