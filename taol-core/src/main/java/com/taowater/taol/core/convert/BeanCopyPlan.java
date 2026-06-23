package com.taowater.taol.core.convert;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 按 (sourceClass, targetClass) 缓存的预编译拷贝计划。
 */
final class BeanCopyPlan {

    private static final ConcurrentMap<String, BeanCopyPlan> CACHE = new ConcurrentHashMap<>();

    private final FieldCopyAction copier;

    private BeanCopyPlan(FieldCopyAction copier) {
        this.copier = copier;
    }

    static BeanCopyPlan create(List<FieldCopyAction> actions) {
        return new BeanCopyPlan(fuse(actions));
    }

    static BeanCopyPlan of(Class<?> sourceClass, Class<?> targetClass) {
        String key = sourceClass.getName() + '\0' + targetClass.getName();
        return CACHE.computeIfAbsent(key, k -> CopyPlanFactory.build(sourceClass, targetClass));
    }

    void copy(Object source, Object target) {
        copier.copy(source, target);
    }

    private static FieldCopyAction fuse(List<FieldCopyAction> actions) {
        if (actions.isEmpty()) {
            return (source, target) -> {
            };
        }
        if (actions.size() == 1) {
            return actions.get(0);
        }
        return fuse(actions.toArray(new FieldCopyAction[0]));
    }

    private static FieldCopyAction fuse(FieldCopyAction[] actions) {
        if (actions.length <= 3) {
            return unroll(actions);
        }
        return (src, tgt) -> {
            for (FieldCopyAction action : actions) {
                action.copy(src, tgt);
            }
        };
    }

    private static FieldCopyAction unroll(FieldCopyAction[] actions) {
        switch (actions.length) {
            case 0:
                return (src, tgt) -> {
                };
            case 1:
                return actions[0];
            case 2: {
                FieldCopyAction a0 = actions[0];
                FieldCopyAction a1 = actions[1];
                return (src, tgt) -> {
                    a0.copy(src, tgt);
                    a1.copy(src, tgt);
                };
            }
            default: {
                FieldCopyAction a0 = actions[0];
                FieldCopyAction a1 = actions[1];
                FieldCopyAction a2 = actions[2];
                return (src, tgt) -> {
                    a0.copy(src, tgt);
                    a1.copy(src, tgt);
                    a2.copy(src, tgt);
                };
            }
        }
    }
}
