package io.github.resilience4j.utils;

import io.github.resilience4j.core.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.Objects;

public class AnnotationExtractor {
    private static final Logger logger = LoggerFactory.getLogger(AnnotationExtractor.class);

    private AnnotationExtractor() {
    }

    /**
     * extract annotation from target class
     *
     * @param targetClass     target class
     * @param annotationClass annotation class
     * @param <T>             The annotation type.
     * @return annotation
     */
    @Nullable
    public static <T extends Annotation> T extract(Class<?> targetClass, Class<T> annotationClass) {
        T annotation = null;
        if (targetClass.isAnnotationPresent(annotationClass)) {
            annotation = targetClass.getAnnotation(annotationClass);
            if (annotation == null && logger.isDebugEnabled()) {
                logger.debug("TargetClass has no annotation '{}'", annotationClass.getSimpleName());
                annotation = targetClass.getDeclaredAnnotation(annotationClass);
                if (annotation == null && logger.isDebugEnabled()) {
                    logger.debug("TargetClass has no declared annotation '{}'", annotationClass.getSimpleName());
                }
            }
        }
        return annotation;
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
    public static <T extends Annotation> T extractAnnotationFromProxy(Object targetProxy, Class<T> annotationClass) {
        if (targetProxy.getClass().getInterfaces().length == 1) {
            return extract(targetProxy.getClass().getInterfaces()[0], annotationClass);
        } else if (targetProxy.getClass().getInterfaces().length > 1) {
            return extractAnnotationFromClosestMatch(targetProxy, annotationClass);
        } else {
            return null;
        }
    }

    @Nullable
    private static <T extends Annotation> T extractAnnotationFromClosestMatch(Object targetProxy, Class<T> annotationClass) {
        int numberOfImplementations = targetProxy.getClass().getInterfaces().length;
        for (int depth = 0; depth < numberOfImplementations; depth++) {
            T annotation = extract(targetProxy.getClass().getInterfaces()[depth], annotationClass);
            if (Objects.nonNull(annotation)) {
                return annotation;
            }
        }
        return null;
    }
}
