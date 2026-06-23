package com.taowater.taol.core.convert;

import com.taowater.taol.core.reflect.ClassUtil;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * 拷贝值转换工具
 */
class CopyValueConverter {

    private static final ConvertedValue UNSUPPORTED = new ConvertedValue(false, null);
    private static final Map<Class<?>, Class<?>> WRAPPER = new HashMap<>();

    static {
        WRAPPER.put(boolean.class, Boolean.class);
        WRAPPER.put(byte.class, Byte.class);
        WRAPPER.put(short.class, Short.class);
        WRAPPER.put(char.class, Character.class);
        WRAPPER.put(int.class, Integer.class);
        WRAPPER.put(long.class, Long.class);
        WRAPPER.put(float.class, Float.class);
        WRAPPER.put(double.class, Double.class);
    }

    static ConvertedValue convert(Object value, Type sourceType, Type targetType, String path) {
        if (value == null || targetType == null) {
            return UNSUPPORTED;
        }

        Class<?> targetRaw = rawClass(targetType);
        if (targetRaw == null) {
            return UNSUPPORTED;
        }

        if (targetRaw.isArray()) {
            return convertToArray(value, sourceType, targetType, path);
        }

        if (isCollectionTarget(targetRaw) && isArrayOrCollection(value)) {
            return convertToCollection(value, sourceType, targetType, targetRaw, path);
        }

        Class<?> targetClass = wrap(targetRaw);
        if (value instanceof Number && isNumberTarget(targetClass)) {
            if (!value.getClass().equals(targetClass)) {
                return new ConvertedValue(true, convertNumber((Number) value, targetClass, path));
            }
            return new ConvertedValue(true, value);
        }

        if (targetClass.isInstance(value)) {
            return new ConvertedValue(true, value);
        }

        return UNSUPPORTED;
    }

    static boolean isNumericType(Type type) {
        Class<?> raw = rawClass(type);
        if (raw == null) {
            return false;
        }
        if (raw.isArray()) {
            Class<?> component = raw.getComponentType();
            return component != null && isNumericComponent(component);
        }
        if (isCollectionTarget(raw) && type instanceof ParameterizedType) {
            Type[] args = ((ParameterizedType) type).getActualTypeArguments();
            return args.length == 1 && isNumericType(args[0]);
        }
        if (raw.isPrimitive()) {
            return isNumericComponent(raw);
        }
        return isNumberTarget(wrap(raw));
    }

    private static boolean isNumericComponent(Class<?> clazz) {
        return clazz == byte.class || clazz == short.class || clazz == int.class || clazz == long.class
                || clazz == float.class || clazz == double.class
                || Number.class.isAssignableFrom(clazz)
                || BigInteger.class.equals(clazz)
                || BigDecimal.class.equals(clazz);
    }

    private static ConvertedValue convertToCollection(Object value, Type sourceType, Type targetType,
                                                      Class<?> targetRaw, String path) {
        Type sourceElementType = elementType(sourceType, value);
        Type targetElementType = elementType(targetType, null);
        Collection<Object> targetCollection = newCollection(targetRaw);
        if (targetCollection == null) {
            return UNSUPPORTED;
        }

        int len = length(value);
        for (int i = 0; i < len; i++) {
            Object item = element(value, i);
            if (item == null) {
                targetCollection.add(null);
                continue;
            }
            ConvertedValue converted = convert(item, sourceElementType, targetElementType, path + "[" + i + "]");
            if (!converted.isSupported()) {
                if (item instanceof Number && isNumericType(targetElementType)) {
                    throw new CopyException("Cannot copy " + path + "[" + i + "]: unsupported numeric conversion from "
                            + item.getClass().getName() + " to " + describeType(targetElementType));
                }
                return UNSUPPORTED;
            }
            targetCollection.add(converted.getValue());
        }
        return new ConvertedValue(true, targetCollection);
    }

    private static ConvertedValue convertToArray(Object value, Type sourceType, Type targetType, String path) {
        if (!isArrayOrCollection(value)) {
            return UNSUPPORTED;
        }

        Type sourceElementType = elementType(sourceType, value);
        Type targetElementType = elementType(targetType, null);
        Class<?> targetComponentClass = rawClass(targetElementType);
        if (targetComponentClass == null) {
            return UNSUPPORTED;
        }

        int len = length(value);
        Object targetArray = Array.newInstance(targetComponentClass, len);
        for (int i = 0; i < len; i++) {
            Object item = element(value, i);
            if (item == null) {
                if (targetComponentClass.isPrimitive()) {
                    throw new CopyException("Cannot copy " + path + "[" + i + "] to primitive array component");
                }
                Array.set(targetArray, i, null);
                continue;
            }
            ConvertedValue converted = convert(item, sourceElementType, targetElementType, path + "[" + i + "]");
            if (!converted.isSupported()) {
                if (item instanceof Number && isNumericComponent(targetComponentClass)) {
                    throw new CopyException("Cannot copy " + path + "[" + i + "]: unsupported numeric conversion from "
                            + item.getClass().getName() + " to " + targetComponentClass.getName());
                }
                return UNSUPPORTED;
            }
            Array.set(targetArray, i, converted.getValue());
        }
        return new ConvertedValue(true, targetArray);
    }

    static Object convertNumber(Number number, Class<?> targetClass, String path) {
        if (Number.class.equals(targetClass)) {
            return number;
        }
        if (BigDecimal.class.equals(targetClass)) {
            return toBigDecimal(number, path);
        }
        if (BigInteger.class.equals(targetClass)) {
            return integralDecimal(number, targetClass, path).toBigIntegerExact();
        }
        if (Byte.class.equals(targetClass)) {
            if (number instanceof Byte) {
                return number;
            }
            if (number instanceof Short || number instanceof Integer || number instanceof Long) {
                return byteFromLong(number.longValue(), path);
            }
            BigDecimal decimal = integralDecimal(number, targetClass, path);
            checkRange(decimal, BigDecimal.valueOf(Byte.MIN_VALUE), BigDecimal.valueOf(Byte.MAX_VALUE), targetClass, path);
            return decimal.byteValue();
        }
        if (Short.class.equals(targetClass)) {
            if (number instanceof Short) {
                return number;
            }
            if (number instanceof Byte) {
                return number.shortValue();
            }
            if (number instanceof Integer || number instanceof Long) {
                return shortFromLong(number.longValue(), path);
            }
            BigDecimal decimal = integralDecimal(number, targetClass, path);
            checkRange(decimal, BigDecimal.valueOf(Short.MIN_VALUE), BigDecimal.valueOf(Short.MAX_VALUE), targetClass, path);
            return decimal.shortValue();
        }
        if (Integer.class.equals(targetClass)) {
            if (number instanceof Integer) {
                return number;
            }
            if (number instanceof Byte || number instanceof Short) {
                return number.intValue();
            }
            if (number instanceof Long) {
                return intFromLong(number.longValue(), path);
            }
            BigDecimal decimal = integralDecimal(number, targetClass, path);
            checkRange(decimal, BigDecimal.valueOf(Integer.MIN_VALUE), BigDecimal.valueOf(Integer.MAX_VALUE), targetClass, path);
            return decimal.intValue();
        }
        if (Long.class.equals(targetClass)) {
            if (number instanceof Long) {
                return number;
            }
            if (number instanceof Byte || number instanceof Short || number instanceof Integer) {
                return number.longValue();
            }
            BigDecimal decimal = integralDecimal(number, targetClass, path);
            checkRange(decimal, BigDecimal.valueOf(Long.MIN_VALUE), BigDecimal.valueOf(Long.MAX_VALUE), targetClass, path);
            return decimal.longValue();
        }
        if (Float.class.equals(targetClass) || Double.class.equals(targetClass)) {
            return toFloatingNumber(number, targetClass, path);
        }
        return number;
    }

    static int intFromLong(long value, String path) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new CopyException("Cannot copy " + path + ": numeric value " + value + " overflows int");
        }
        return (int) value;
    }

    static short shortFromLong(long value, String path) {
        if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
            throw new CopyException("Cannot copy " + path + ": numeric value " + value + " overflows short");
        }
        return (short) value;
    }

    static byte byteFromLong(long value, String path) {
        if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            throw new CopyException("Cannot copy " + path + ": numeric value " + value + " overflows byte");
        }
        return (byte) value;
    }

    static short shortFromInt(int value, String path) {
        if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
            throw new CopyException("Cannot copy " + path + ": numeric value " + value + " overflows short");
        }
        return (short) value;
    }

    static byte byteFromInt(int value, String path) {
        if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            throw new CopyException("Cannot copy " + path + ": numeric value " + value + " overflows byte");
        }
        return (byte) value;
    }

    static byte byteFromShort(short value, String path) {
        if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            throw new CopyException("Cannot copy " + path + ": numeric value " + value + " overflows byte");
        }
        return (byte) value;
    }

    static float floatFromDouble(double value, String path) {
        float result = (float) value;
        if (Float.isNaN(result) || Float.isInfinite(result)) {
            throw new CopyException("Cannot copy " + path + ": numeric value " + value + " overflows float");
        }
        return result;
    }

    static Class<?> wrapClass(Class<?> clazz) {
        return wrap(clazz);
    }

    /**
     * 整型拓宽到浮点：在 IEEE-754 可表达范围内尽量保留精度（long 恒可转为有限 double）。
     */
    private static Number toFloatingNumber(Number number, Class<?> targetClass, String path) {
        double doubleValue = toDoubleValue(number, path);
        if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
            throw new CopyException("Cannot copy " + path + ": numeric value " + number
                    + " overflows " + targetClass.getName());
        }
        if (Float.class.equals(targetClass)) {
            float floatValue = (float) doubleValue;
            if (Float.isNaN(floatValue) || Float.isInfinite(floatValue)) {
                throw new CopyException("Cannot copy " + path + ": numeric value " + number
                        + " overflows " + targetClass.getName());
            }
            return floatValue;
        }
        return doubleValue;
    }

    private static double toDoubleValue(Number number, String path) {
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            // long 在 double 可表达范围内恒为有限值，按 IEEE-754 尽量保留精度
            return (double) number.longValue();
        }
        if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number).doubleValue();
        }
        if (number instanceof BigDecimal) {
            return ((BigDecimal) number).doubleValue();
        }
        return number.doubleValue();
    }

    private static BigDecimal integralDecimal(Number number, Class<?> targetClass, String path) {
        BigDecimal decimal = toBigDecimal(number, path);
        try {
            return new BigDecimal(decimal.toBigIntegerExact());
        } catch (ArithmeticException e) {
            throw new CopyException("Cannot copy " + path + ": numeric value " + number
                    + " is not an integer for " + targetClass.getName(), e);
        }
    }

    private static BigDecimal toBigDecimal(Number number, String path) {
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        }
        if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        }
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            return BigDecimal.valueOf(number.longValue());
        }
        if (number instanceof Float || number instanceof Double) {
            double value = number.doubleValue();
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new CopyException("Cannot copy " + path + ": numeric value " + number + " is not finite");
            }
            return BigDecimal.valueOf(value);
        }
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException e) {
            throw new CopyException("Cannot copy " + path + ": numeric value " + number + " is not supported", e);
        }
    }

    private static void checkRange(BigDecimal value, BigDecimal min, BigDecimal max,
                                   Class<?> targetClass, String path) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new CopyException("Cannot copy " + path + ": numeric value " + value.toPlainString()
                    + " overflows " + targetClass.getName());
        }
    }

    private static String describeType(Type type) {
        Class<?> raw = rawClass(type);
        return raw == null ? String.valueOf(type) : raw.getName();
    }

    private static Collection<Object> newCollection(Class<?> targetRaw) {
        if (targetRaw.isInterface() || Modifier.isAbstract(targetRaw.getModifiers())) {
            if (SortedSet.class.isAssignableFrom(targetRaw) || NavigableSet.class.isAssignableFrom(targetRaw)) {
                return new TreeSet<>();
            }
            if (Set.class.isAssignableFrom(targetRaw)) {
                return new LinkedHashSet<>();
            }
            if (Queue.class.isAssignableFrom(targetRaw) || Deque.class.isAssignableFrom(targetRaw)) {
                return new LinkedList<>();
            }
            if (Collection.class.isAssignableFrom(targetRaw) || Iterable.class.equals(targetRaw)) {
                return new ArrayList<>();
            }
            return null;
        }

        Object instance;
        try {
            instance = ClassUtil.newInstance(targetRaw);
        } catch (RuntimeException e) {
            return null;
        }
        if (instance instanceof Collection) {
            return (Collection<Object>) instance;
        }
        return null;
    }

    private static boolean isArrayOrCollection(Object value) {
        return value != null && (value.getClass().isArray() || value instanceof Collection);
    }

    private static boolean isCollectionTarget(Class<?> rawClass) {
        return Collection.class.isAssignableFrom(rawClass) || Iterable.class.equals(rawClass);
    }

    private static boolean isNumberTarget(Class<?> targetClass) {
        return Number.class.isAssignableFrom(targetClass)
                || BigInteger.class.equals(targetClass)
                || BigDecimal.class.equals(targetClass);
    }

    private static int length(Object value) {
        if (value.getClass().isArray()) {
            return Array.getLength(value);
        }
        return ((Collection<?>) value).size();
    }

    private static Object element(Object value, int index) {
        if (value.getClass().isArray()) {
            return Array.get(value, index);
        }
        if (value instanceof List) {
            return ((List<?>) value).get(index);
        }
        int i = 0;
        for (Object item : (Collection<?>) value) {
            if (i++ == index) {
                return item;
            }
        }
        return null;
    }

    private static Type elementType(Type type, Object value) {
        if (type instanceof ParameterizedType) {
            Type[] args = ((ParameterizedType) type).getActualTypeArguments();
            if (args.length == 1) {
                return normalizeType(args[0]);
            }
        }
        if (type instanceof GenericArrayType) {
            return normalizeType(((GenericArrayType) type).getGenericComponentType());
        }
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                return clazz.getComponentType();
            }
        }
        if (value != null && value.getClass().isArray()) {
            return value.getClass().getComponentType();
        }
        return Object.class;
    }

    private static Type normalizeType(Type type) {
        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length > 0) {
                return normalizeType(upperBounds[0]);
            }
            return Object.class;
        }
        if (type instanceof TypeVariable) {
            TypeVariable<?> typeVariable = (TypeVariable<?>) type;
            Type[] bounds = typeVariable.getBounds();
            if (bounds.length > 0) {
                return normalizeType(bounds[0]);
            }
            return Object.class;
        }
        return type;
    }

    private static Class<?> rawClass(Type type) {
        Type normalizedType = normalizeType(type);
        if (normalizedType instanceof Class<?>) {
            return (Class<?>) normalizedType;
        }
        if (normalizedType instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) normalizedType).getRawType();
            if (rawType instanceof Class<?>) {
                return (Class<?>) rawType;
            }
        }
        if (normalizedType instanceof GenericArrayType) {
            Class<?> componentClass = rawClass(((GenericArrayType) normalizedType).getGenericComponentType());
            if (componentClass != null) {
                return Array.newInstance(componentClass, 0).getClass();
            }
        }
        return null;
    }

    private static Class<?> wrap(Class<?> clazz) {
        if (clazz == null || !clazz.isPrimitive()) {
            return clazz;
        }
        return Objects.requireNonNull(WRAPPER.get(clazz), "unsupported primitive: " + clazz);
    }

    static class ConvertedValue {
        private final boolean supported;
        private final Object value;

        ConvertedValue(boolean supported, Object value) {
            this.supported = supported;
            this.value = value;
        }

        boolean isSupported() {
            return supported;
        }

        Object getValue() {
            return value;
        }
    }
}
