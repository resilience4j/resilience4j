package io.github.resilience4j.core;

import io.github.resilience4j.core.lang.Nullable;

import java.lang.reflect.Constructor;
import java.util.function.Predicate;

public class ClassUtils {

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> instantiatePredicateClass(Class<? extends Predicate<T>> clazz) {
        try {
            Constructor<? extends Predicate> c = clazz.getConstructor();
            if (c != null) {
                return c.newInstance();
            } else {
                throw new InstantiationException(
                    "Unable to create instance of class: " + clazz.getName());
            }
        } catch (Exception e) {
            throw new InstantiationException(
                "Unable to create instance of class: " + clazz.getName(), e);
        }
    }
}
