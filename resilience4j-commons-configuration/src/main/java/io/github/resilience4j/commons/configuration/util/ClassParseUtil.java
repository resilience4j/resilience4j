package io.github.resilience4j.commons.configuration.util;

import java.lang.reflect.Array;
import java.util.List;
import java.util.stream.Collectors;

public class ClassParseUtil {
    public static <T> Class<? extends T>[] convertStringListToClassTypeArray(List<String> classNames, Class<? extends T> targetClassType) {
        Class<? extends T>[] array = (Class<? extends T>[]) Array.newInstance(Class.class, classNames.size());

        return classNames.stream()
                .map(className -> (Class<? extends T>) convertStringToClassType(className, targetClassType))
                .collect(Collectors.toList())
                .toArray(array);
    }

    public static <T> Class<? extends T> convertStringToClassType(String className, Class<? extends T> targetClassType) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!targetClassType.isAssignableFrom(clazz)){
                throw new ConfigParseException("Class " + className + " is not a subclass of " + targetClassType.getName());
            }
            return (Class<? extends T>) clazz;
        } catch (ClassNotFoundException e) {
            throw new ConfigParseException("Class not found: " + className, e);
        }
    }
}
