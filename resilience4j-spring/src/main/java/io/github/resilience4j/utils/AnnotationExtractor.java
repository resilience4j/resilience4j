package io.github.resilience4j.utils;

import io.github.resilience4j.core.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;

public class AnnotationExtractor {
    private static final Logger logger = LoggerFactory.getLogger(AnnotationExtractor.class);

    private AnnotationExtractor() {
    }

    /**
     * extract annotation from target class
     *
     * @param targetClass target class
     * @param annotationClass annotation class
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
}
