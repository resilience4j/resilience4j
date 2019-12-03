package io.github.resilience4j.core;

import io.github.resilience4j.core.lang.Nullable;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
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

    public static <T> T instantiateClassDefConstructor(Class<T> clazz) {
        //if constructor present then it should have a no arg constructor
        //if not present then default constructor is already their
        Objects.requireNonNull(clazz, "class to instantiate should not be null");
        if (clazz.getConstructors().length > 0 && !Arrays.stream(clazz.getConstructors())
            .filter(c -> c.getParameterCount() == 0)
            .findFirst().isPresent()) {
            throw new InstantiationException(
                "Default constructor is required to create instance of public class: " + clazz
                    .getName());
        }
        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new InstantiationException(
                "Unable to create instance of class: " + clazz.getName(), e);
        }
    }
}
