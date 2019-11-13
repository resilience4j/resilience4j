package io.github.resilience4j.utils;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.utils.AnnotationExtractor;
import io.vavr.CheckedFunction0;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProceedingJoinPointHelper {

    private static final Logger logger = LoggerFactory.getLogger(ProceedingJoinPointHelper.class);

    private final ProceedingJoinPoint joinPoint;
    private final Method method;
    private final Method declaringMethod;
    private final String declaringMethodName;
    private final Class<?> returnType;
    private CheckedFunction0<Object> decoratedProceedCall;

    /**
     * @param proceedCall Spring AOP call to {@link ProceedingJoinPoint#proceed()} with possible
     * decorators already applied
     */
    private ProceedingJoinPointHelper(
        ProceedingJoinPoint joinPoint,
        Method method,
        Method declaringMethod,
        CheckedFunction0<Object> proceedCall) {
        this.joinPoint = joinPoint;
        this.decoratedProceedCall = proceedCall;
        this.method = method;
        this.declaringMethod = declaringMethod;
        declaringMethodName = declaringMethod.getDeclaringClass().getName() + "#" + method.getName();
        returnType = declaringMethod.getReturnType();
    }

    public static ProceedingJoinPointHelper prepareFor(ProceedingJoinPoint joinPoint) {
        try {
            Method declaringMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
            Method method = joinPoint.getTarget().getClass().getMethod(
                declaringMethod.getName(), declaringMethod.getParameterTypes());
            CheckedFunction0<Object> proceedCall = () -> joinPoint.proceed();
            return new ProceedingJoinPointHelper(
                joinPoint, method, declaringMethod, proceedCall);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                "Target without a method from its interface / superclass?", e);
        }
    }

    public ProceedingJoinPoint getJoinPoint() {
        return joinPoint;
    }

    public Method getMethod() {
        return method;
    }

    public Method getDeclaringMethod() {
        return declaringMethod;
    }

    public String getDeclaringMethodName() {
        return declaringMethodName;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    /**
     * Spring AOP call to {@link ProceedingJoinPoint#proceed()} with possible decorators already
     * applied
     */
    public CheckedFunction0<Object> getDecoratedProceedCall() {
        return decoratedProceedCall;
    }

    public void decorateProceedCall(
        Function<CheckedFunction0<Object>,
            CheckedFunction0<Object>> decorator) {
        decoratedProceedCall = decorator.apply(decoratedProceedCall);
    }

    public <T extends Annotation> List<T> getMethodAnnotations(Class<T> annotationClass) {
        if (joinPoint.getTarget() instanceof Proxy) {
            return AnnotationExtractor.extractAllMethodAnnotationsFromProxy(
                joinPoint.getTarget(), declaringMethod, annotationClass);
        } else {
            return Arrays.asList(getMethod().getAnnotationsByType(annotationClass));
        }
    }

    @Nullable
    public <T extends Annotation> T getClassAnnotation(Class<T> annotationClass) {
        return getClassAnnotations(annotationClass)
            .stream().findFirst().orElse(null);
    }

    @Nullable
    public <T extends Annotation> List<T> getClassAnnotations(Class<T> annotationClass) {
        if (joinPoint.getTarget() instanceof Proxy) {
            return AnnotationExtractor.extractAllAnnotationsFromProxy(
                joinPoint.getTarget(), annotationClass);
        } else {
            return AnnotationExtractor.extractAll(
                joinPoint.getTarget().getClass(), annotationClass);
        }
    }
}
