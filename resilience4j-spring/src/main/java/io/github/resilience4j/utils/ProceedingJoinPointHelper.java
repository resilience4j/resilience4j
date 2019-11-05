package io.github.resilience4j.utils;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.utils.AnnotationExtractor;
import io.vavr.CheckedFunction0;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProceedingJoinPointHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(ProceedingJoinPointHelper.class);

    private final ProceedingJoinPoint joinPoint;
    private final Method method;
    private final String methodName;
    private final Class<?> returnType;
    private CheckedFunction0<Object> decoratedProceedCall;

    public ProceedingJoinPointHelper(ProceedingJoinPoint joinPoint) {
        this(joinPoint, () -> joinPoint.proceed());
    }
    
    /**
     * @param proceedCall Spring AOP call to {@link ProceedingJoinPoint#proceed()} with possible decorators already applied
     */
    public ProceedingJoinPointHelper(ProceedingJoinPoint joinPoint, CheckedFunction0<Object> proceedCall) {
        this(joinPoint, ((MethodSignature) joinPoint.getSignature()).getMethod(), proceedCall);
    }
    
    public ProceedingJoinPointHelper(ProceedingJoinPoint joinPoint, Method method) {
        this(joinPoint, method, () -> joinPoint.proceed());
    }
    
    public ProceedingJoinPointHelper(ProceedingJoinPoint joinPoint, Method method, CheckedFunction0<Object> proceedCall) {
        this.joinPoint = joinPoint;
        this.decoratedProceedCall = proceedCall;
        this.method = method;
        methodName = method.getDeclaringClass().getName() + "#" + method.getName();
        returnType = method.getReturnType();
    }

    public ProceedingJoinPoint getJoinPoint() {
        return joinPoint;
    }

    public Method getMethod() {
        return method;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    /**
     * Spring AOP call to {@link ProceedingJoinPoint#proceed()} with possible decorators already applied
     */
    public CheckedFunction0<Object> getDecoratedProceedCall() {
        return decoratedProceedCall;
    }
    
    public void decorateProceedCall(Function<CheckedFunction0<Object>, CheckedFunction0<Object>> decorator) {
        decoratedProceedCall = decorator.apply(decoratedProceedCall);
    }

    public void setDecoratedProceedCall(CheckedFunction0<Object> decoratedProceedCall) {
        this.decoratedProceedCall = decoratedProceedCall;
    }

    @Nullable
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (joinPoint.getTarget() instanceof Proxy) {
            logger.debug("The rate limiter annotation is kept on a interface which is acting as a proxy");
            return AnnotationExtractor.extractAnnotationFromProxy(joinPoint.getTarget(), annotationClass);
        } else {
            return AnnotationExtractor.extract(joinPoint.getTarget().getClass(), annotationClass);
        }
    }
}
