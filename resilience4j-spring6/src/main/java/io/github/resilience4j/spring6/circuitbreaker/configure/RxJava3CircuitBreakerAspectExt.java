package io.github.resilience4j.spring6.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.rxjava3.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.spring6.utils.AspectUtil;
import io.reactivex.rxjava3.core.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * the Rx circuit breaker logic support for the spring AOP conditional on the presence of Rx classes
 * on the spring class loader
 */
public class RxJava3CircuitBreakerAspectExt implements CircuitBreakerAspectExt {

    private static final Logger logger = LoggerFactory
        .getLogger(RxJava3CircuitBreakerAspectExt.class);
    private final Set<Class> rxSupportedTypes = AspectUtil.newHashSet(ObservableSource.class,
        SingleSource.class, CompletableSource.class, MaybeSource.class, Flowable.class);

    /**
     * @param returnType the AOP method return type class
     * @return boolean if the method has Rx java 3 rerun type
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean canHandleReturnType(Class returnType) {
        return rxSupportedTypes.stream()
            .anyMatch(classType -> classType.isAssignableFrom(returnType));
    }

    /**
     * @param proceedingJoinPoint Spring AOP proceedingJoinPoint
     * @param circuitBreaker      the configured circuitBreaker
     * @param methodName          the method name
     * @return the result object
     * @throws Throwable exception in case of faulty flow
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object handle(ProceedingJoinPoint proceedingJoinPoint, CircuitBreaker circuitBreaker,
        String methodName) throws Throwable {
        CircuitBreakerOperator circuitBreakerOperator = CircuitBreakerOperator.of(circuitBreaker);
        Object returnValue = proceedingJoinPoint.proceed();

        return executeRxJava3Aspect(circuitBreakerOperator, returnValue, methodName);
    }

    @SuppressWarnings("unchecked")
    private Object executeRxJava3Aspect(CircuitBreakerOperator circuitBreakerOperator,
        Object returnValue, String methodName) {
        if (returnValue instanceof ObservableSource) {
            Observable<?> observable = (Observable) returnValue;
            return observable.compose(circuitBreakerOperator);
        } else if (returnValue instanceof SingleSource) {
            Single<?> single = (Single) returnValue;
            return single.compose(circuitBreakerOperator);
        } else if (returnValue instanceof CompletableSource) {
            Completable completable = (Completable) returnValue;
            return completable.compose(circuitBreakerOperator);
        } else if (returnValue instanceof MaybeSource) {
            Maybe<?> maybe = (Maybe) returnValue;
            return maybe.compose(circuitBreakerOperator);
        } else if (returnValue instanceof Flowable) {
            Flowable<?> flowable = (Flowable) returnValue;
            return flowable.compose(circuitBreakerOperator);
        } else {
            logger
                .error("Unsupported type for RxJava3 circuit breaker return type {} for method {}",
                    returnValue.getClass().getTypeName(), methodName);
            throw new IllegalArgumentException(
                "Not Supported type for the circuit breaker in RxJava3:" + returnValue.getClass()
                    .getName());
        }
    }
}
