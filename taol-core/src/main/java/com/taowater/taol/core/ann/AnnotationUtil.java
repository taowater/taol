package com.taowater.taol.core.ann;

import lombok.experimental.UtilityClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * 注解工具
 */
@UtilityClass
public class AnnotationUtil {

    public static <A extends Annotation> A getAnnotation(AnnotatedElement el, Class<A> type) {
        A direct = el.getAnnotation(type);
        if (direct != null) {
            return direct;
        }

        // 找 meta-annotation
        Annotation source = null;
        for (Annotation ann : el.getAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(type)) {
                source = ann;
                break;
            }
        }
        if (source == null) {
            return null;
        }

        // 拿最顶层 Ann（meta 注解上的 Ann）
        A metaAnn = source.annotationType().getAnnotation(type);

        // 收集属性
        Map<String, Object> attributes = new HashMap<>();
        mergeAttributes(metaAnn, attributes);
        mergeAttributes(source, attributes);

        // alias 同义词组处理
        resolveAliasFor(source, type, attributes);
        resolveAliasFor(metaAnn, type, attributes);

        // 构造代理对象
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class[]{type},
                new AnnHandler(type, attributes)
        ));
    }

    private static void mergeAttributes(Annotation ann, Map<String, Object> attributes) {
        if (ann == null) {
            return;
        }
        for (Method m : ann.annotationType().getDeclaredMethods()) {
            try {
                attributes.put(m.getName(), m.invoke(ann));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 解析 alias 形成同义词组
     */
    private static void resolveAliasFor(Annotation ann,
                                        Class<? extends Annotation> targetType,
                                        Map<String, Object> attributes) {
        if (ann == null) {
            return;
        }

        Map<String, List<String>> group = new HashMap<>();
        Map<String, Object> explicitValues = new HashMap<>();

        Class<?> annType = ann.annotationType();

        for (Method m : annType.getDeclaredMethods()) {
            AliasFor alias = m.getAnnotation(AliasFor.class);
            if (alias == null) {
                continue;
            }

            // alias 注解目标是 Ann
            if (alias.annotation() != targetType) {
                continue;
            }

            String aliasAttr = m.getName();
            String targetAttr = alias.value();

            group.computeIfAbsent(targetAttr, k -> new ArrayList<>()).add(aliasAttr);

            // 获取属性值（显式值）
            try {
                Object value = m.invoke(ann);
                Object def = m.getDefaultValue();
                if (!Objects.equals(value, def)) {
                    explicitValues.put(aliasAttr, value);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // 同义词组内冲突检测 + 统一赋值
        for (String targetAttr : group.keySet()) {
            List<String> aliases = group.get(targetAttr);

            Object winner = null;

            for (String aliasAttr : aliases) {
                if (explicitValues.containsKey(aliasAttr)) {
                    Object v = explicitValues.get(aliasAttr);

                    if (winner == null) {
                        winner = v;
                    } else if (!winner.equals(v)) {
                        throw new IllegalStateException("Alias conflict for " + targetAttr);
                    }
                }
            }

            if (winner != null) {
                attributes.put(targetAttr, winner);
            }
        }
    }

    private static class AnnHandler implements InvocationHandler {
        private final Class<? extends Annotation> type;
        private final Map<String, Object> attributes;

        AnnHandler(Class<? extends Annotation> type, Map<String, Object> attributes) {
            this.type = type;
            this.attributes = attributes;
        }

        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if (name.equals("annotationType")) {
                return type;
            }
            return attributes.getOrDefault(name, method.getDefaultValue());
        }
    }
}
