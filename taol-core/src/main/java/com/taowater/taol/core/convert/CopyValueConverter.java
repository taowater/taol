package com.taowater.taol.core.convert;

import com.taowater.taol.core.reflect.ClassUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * 运行时值转换：数值窄化/拓宽、数组与集合元素递归转换。
 * <p>
 * fast path 未覆盖的组合由此兜底；{@link CopyPlanFactory#genericConvertAction} 在运行时调用 {@link #convert}。
 */
@SuppressWarnings("unchecked")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

    /**
     * 单值转换入口。支持：数组、集合、数值、isInstance 直传；不支持则返回 UNSUPPORTED。
     *
     * @param path 字段名或索引路径，用于异常信息
     */
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

    /**
     * 判断类型（含数组元素、集合泛型）是否为数值类型。
     */
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
            String itemPath = path + "[" + i + "]";
            Object item = element(value, i);
            if (item == null) {
                targetCollection.add(null);
                continue;
            }
            ConvertedValue converted = convertElement(item, sourceElementType, targetElementType, itemPath,
                    isNumericType(targetElementType));
            if (!converted.isSupported()) {
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
            String itemPath = path + "[" + i + "]";
            Object item = element(value, i);
            if (item == null) {
                if (targetComponentClass.isPrimitive()) {
                    throw new CopyException("Cannot copy " + itemPath + " to primitive array component");
                }
                Array.set(targetArray, i, null);
                continue;
            }
            ConvertedValue converted = convertElement(item, sourceElementType, targetElementType, itemPath,
                    isNumericComponent(targetComponentClass));
            if (!converted.isSupported()) {
                return UNSUPPORTED;
            }
            Array.set(targetArray, i, converted.getValue());
        }
        return new ConvertedValue(true, targetArray);
    }

    /**
     * 数组/集合元素转换；数值目标且无法转换时抛 {@link CopyException}。
     */
    private static ConvertedValue convertElement(Object item, Type sourceElementType, Type targetElementType,
                                                 String itemPath, boolean numericTarget) {
        ConvertedValue converted = convert(item, sourceElementType, targetElementType, itemPath);
        if (converted.isSupported()) {
            return converted;
        }
        if (item instanceof Number && numericTarget) {
            throw new CopyException("Cannot copy " + itemPath + ": unsupported numeric conversion from "
                    + item.getClass().getName() + " to " + describeType(targetElementType));
        }
        return UNSUPPORTED;
    }

    /**
     * 数值转换核心。窄化溢出抛 {@link CopyException}，path 为字段名。
     * 被 fast path 兜底（{@link CopyPlanFactory#numericConvertAction}）和 {@link #convert} 共用。
     */
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
            return toByte(number, path);
        }
        if (Short.class.equals(targetClass)) {
            return toShort(number, path);
        }
        if (Integer.class.equals(targetClass)) {
            return toInteger(number, path);
        }
        if (Long.class.equals(targetClass)) {
            return toLong(number, path);
        }
        if (Float.class.equals(targetClass) || Double.class.equals(targetClass)) {
            return toFloatingNumber(number, targetClass, path);
        }
        return number;
    }

    private static Number toByte(Number number, String path) {
        if (number instanceof Byte) {
            return number;
        }
        if (number instanceof Short || number instanceof Integer || number instanceof Long) {
            return byteFromLong(number.longValue(), path);
        }
        return convertIntegralWithRange(number, Byte.class, path,
                BigDecimal.valueOf(Byte.MIN_VALUE), BigDecimal.valueOf(Byte.MAX_VALUE), BigDecimal::byteValue);
    }

    private static Number toShort(Number number, String path) {
        if (number instanceof Short) {
            return number;
        }
        if (number instanceof Byte) {
            return number.shortValue();
        }
        if (number instanceof Integer || number instanceof Long) {
            return shortFromLong(number.longValue(), path);
        }
        return convertIntegralWithRange(number, Short.class, path,
                BigDecimal.valueOf(Short.MIN_VALUE), BigDecimal.valueOf(Short.MAX_VALUE), BigDecimal::shortValue);
    }

    private static Number toInteger(Number number, String path) {
        if (number instanceof Integer) {
            return number;
        }
        if (number instanceof Byte || number instanceof Short) {
            return number.intValue();
        }
        if (number instanceof Long) {
            return intFromLong(number.longValue(), path);
        }
        return convertIntegralWithRange(number, Integer.class, path,
                BigDecimal.valueOf(Integer.MIN_VALUE), BigDecimal.valueOf(Integer.MAX_VALUE), BigDecimal::intValue);
    }

    private static Number toLong(Number number, String path) {
        if (number instanceof Long) {
            return number;
        }
        if (number instanceof Byte || number instanceof Short || number instanceof Integer) {
            return number.longValue();
        }
        return convertIntegralWithRange(number, Long.class, path,
                BigDecimal.valueOf(Long.MIN_VALUE), BigDecimal.valueOf(Long.MAX_VALUE), BigDecimal::longValue);
    }

    private static Number convertIntegralWithRange(Number number, Class<?> targetClass, String path,
                                                   BigDecimal min, BigDecimal max,
                                                   java.util.function.Function<BigDecimal, Number> extractor) {
        BigDecimal decimal = integralDecimal(number, targetClass, path);
        checkRange(decimal, min, max, targetClass, path);
        return extractor.apply(decimal);
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

    /**
     * 基本类型 → 包装类，供 numericConvertAction 等使用。
     */
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
            return newDefaultCollection(targetRaw);
        }
        return newConcreteCollection(targetRaw);
    }

    /**
     * 接口/抽象集合类型映射到默认实现。
     */
    private static Collection<Object> newDefaultCollection(Class<?> targetRaw) {
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

    private static Collection<Object> newConcreteCollection(Class<?> targetRaw) {
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

    /**
     * 解析泛型/通配符/数组，得到运行时 Class。
     */
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

    /**
     * convert 结果包装：supported=false 表示无法转换。
     */
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
