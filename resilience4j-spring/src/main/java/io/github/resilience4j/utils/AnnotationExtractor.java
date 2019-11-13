package io.github.resilience4j.utils;

import io.github.resilience4j.core.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AnnotationExtractor {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationExtractor.class);

    private AnnotationExtractor() {
    }

    /**
     * extract annotation from target class
     *
     * @param targetClass target class
     * @param annotationClass annotation class
     * @param <T> The annotation type.
     * @return annotation
     */
    @Nullable
    public static <T extends Annotation> T extract(
        Class<?> targetClass, Class<T> annotationClass) {
        return extractAll(targetClass, annotationClass)
            .stream().findFirst().orElse(null);
    }

    /**
     * extract annotations from target class
     *
     * @param targetClass target class
     * @param annotationClass annotation class
     * @param <T> The annotation type.
     * @return annotation
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> List<T> extractAll(
        Class<?> targetClass, Class<T> annotationClass) {
        List<T> annotations = Collections.EMPTY_LIST;
        if (targetClass.isAnnotationPresent(annotationClass)) {
            annotations = Arrays.asList(targetClass.getAnnotationsByType(annotationClass));
            if (logger.isDebugEnabled() && annotations.isEmpty()) {
                logger.debug(
                    "TargetClass has no annotation '{}'",
                    annotationClass.getSimpleName());
                annotations = Arrays.asList(targetClass.getDeclaredAnnotationsByType(annotationClass));
                if (logger.isDebugEnabled() && annotations.isEmpty()) {
                    logger.debug(
                        "TargetClass has no declared annotation '{}'",
                        annotationClass.getSimpleName());
                }
            }
        }
        return annotations;
    }

    /**
     * Extracts the annotation from the target implementation of the Proxy(ies)
     *
     * @param targetProxy The proxy class
     * @param annotationClass The annotation to extract
     * @param <T>
     * @return
     */
    @Nullable
    public static <T extends Annotation> T extractAnnotationFromProxy(
        Object targetProxy, Class<T> annotationClass) {
        return extractAllAnnotationsFromProxy(targetProxy, annotationClass)
            .stream().findFirst().orElse(null);
    }

    /**
     * Extracts annotations from the target implementation of the Proxy(ies)
     *
     * @param targetProxy The proxy class
     * @param annotationClass The annotation to extract
     * @param <T>
     * @return
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> List<T> extractAllAnnotationsFromProxy(
        Object targetProxy, Class<T> annotationClass) {
        if (targetProxy.getClass().getInterfaces().length == 1) {
            return extractAll(targetProxy.getClass().getInterfaces()[0], annotationClass);
        } else if (targetProxy.getClass().getInterfaces().length > 1) {
            return extractAllAnnotationsFromClosestMatch(targetProxy, annotationClass);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Extracts the annotation from the target implementation of the Proxy(ies)
     *
     * @param targetProxy The proxy class
     * @param annotationClass The annotation to extract
     * @param <T>
     * @return
     */
    @Nullable
    public static <T extends Annotation> List<T> extractAllMethodAnnotationsFromProxy(
        Object targetProxy, Method method, Class<T> annotationClass) {
        Class<?>[] interfaces = targetProxy.getClass().getInterfaces();
        for (int depth = 0; depth < interfaces.length; depth++) {
            List<T> annotations = extractAllMethodAnnotationsIfMethodExistsInClass(
                interfaces[depth], method, annotationClass);
            if (!annotations.isEmpty()) {
                return annotations;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Annotation> List<T> extractAllAnnotationsFromClosestMatch(
        Object targetProxy, Class<T> annotationClass) {
        int numberOfImplementations = targetProxy.getClass().getInterfaces().length;
        for (int depth = 0; depth < numberOfImplementations; depth++) {
            List<T> annotations = extractAll(
                targetProxy.getClass().getInterfaces()[depth], annotationClass);
            if (!annotations.isEmpty()) {
                return annotations;
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> List<T> extractAllMethodAnnotationsIfMethodExistsInClass(
        Class<?> interfaceClass, Method overridenMethod, Class<T> annotationClass) {
        try {
            Method classesMethod = interfaceClass.getMethod(
                overridenMethod.getName(), overridenMethod.getParameterTypes());
            return Arrays.asList(classesMethod.getAnnotationsByType(annotationClass));
        } catch (NoSuchMethodException e) {
            return Collections.EMPTY_LIST;
        }
    }
}
