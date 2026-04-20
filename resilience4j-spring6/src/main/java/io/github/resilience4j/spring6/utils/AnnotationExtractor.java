package io.github.resilience4j.spring6.utils;

import io.github.resilience4j.core.lang.Nullable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationExtractor {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationExtractor.class);
    private static final Map<AnnotationCacheKey, Optional<? extends Annotation>> MERGED_ANNOTATION_CACHE = new ConcurrentHashMap<>();

    private AnnotationExtractor() {
    }

    /**
     * Extract annotation from join point method/class and fallback to proxy/interface lookup.
     *
     * @param proceedingJoinPoint proceedingJoinPoint
     * @param annotationClass annotation class
     * @param <T> The annotation type.
     * @return annotation
     */
    @Nullable
    public static <T extends Annotation> T extractAnnotationFromJoinPoint(
        ProceedingJoinPoint proceedingJoinPoint, Class<T> annotationClass) {
        Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
        Class<?> targetClass = proceedingJoinPoint.getTarget() != null
            ? proceedingJoinPoint.getTarget().getClass()
            : method.getDeclaringClass();
        Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);

        T annotation = findMergedAnnotation(targetMethod, annotationClass);
        if (annotation != null) {
            return annotation;
        }

        annotation = findMergedAnnotation(targetClass, annotationClass);
        if (annotation != null) {
            return annotation;
        }

        if (proceedingJoinPoint.getTarget() instanceof Proxy) {
            return extractAnnotationFromProxy(proceedingJoinPoint.getTarget(), annotationClass);
        } else {
            return extract(targetClass, annotationClass);
        }
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
                    logger.debug("TargetClass has no declared annotation '{}'",
                        annotationClass.getSimpleName());
                }
            }
        }
        return annotation;
    }

    /**
     * Extracts the annotation from the target implementation of the Proxy(ies)
     *
     * @param targetProxy     The proxy class
     * @param annotationClass The annotation to extract
     * @param <T>
     * @return
     */
    @Nullable
    public static <T extends Annotation> T extractAnnotationFromProxy(Object targetProxy,
        Class<T> annotationClass) {
        if (targetProxy.getClass().getInterfaces().length == 1) {
            return extract(targetProxy.getClass().getInterfaces()[0], annotationClass);
        } else if (targetProxy.getClass().getInterfaces().length > 1) {
            return extractAnnotationFromClosestMatch(targetProxy, annotationClass);
        } else {
            return null;
        }
    }

    @Nullable
    private static <T extends Annotation> T extractAnnotationFromClosestMatch(Object targetProxy,
        Class<T> annotationClass) {
        int numberOfImplementations = targetProxy.getClass().getInterfaces().length;
        for (int depth = 0; depth < numberOfImplementations; depth++) {
            T annotation = extract(targetProxy.getClass().getInterfaces()[depth], annotationClass);
            if (Objects.nonNull(annotation)) {
                return annotation;
            }
        }
        return null;
    }

    @Nullable
    private static <T extends Annotation> T findMergedAnnotation(AnnotatedElement annotatedElement,
        Class<T> annotationClass) {
        AnnotationCacheKey cacheKey = new AnnotationCacheKey(annotatedElement, annotationClass);
        @SuppressWarnings("unchecked")
        Optional<T> cached = (Optional<T>) MERGED_ANNOTATION_CACHE.computeIfAbsent(cacheKey,
            ignored -> Optional.ofNullable(
                AnnotatedElementUtils.findMergedAnnotation(annotatedElement, annotationClass)));
        return cached.orElse(null);
    }

    private record AnnotationCacheKey(AnnotatedElement annotatedElement,
                                      Class<? extends Annotation> annotationClass) {
    }
}
