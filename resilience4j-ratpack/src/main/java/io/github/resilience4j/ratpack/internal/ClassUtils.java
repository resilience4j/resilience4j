package io.github.resilience4j.ratpack.internal;

import io.github.resilience4j.core.lang.Nullable;

import java.lang.reflect.Constructor;
import java.util.function.Predicate;

public class ClassUtils {

    @Nullable
    public static Predicate instantiatePredicateClass(Class<? extends Predicate> clazz) {
        try {
            Constructor<? extends Predicate> c = clazz.getConstructor();
            if (c != null) {
                return c.newInstance();
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to create instance of class: " + clazz.getName());
        }
    }
}
