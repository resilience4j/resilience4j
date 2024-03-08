package io.github.resilience4j.ratelimiter.configure;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.rxjava3.ratelimiter.operator.RateLimiterOperator;
import io.reactivex.rxjava3.core.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static io.github.resilience4j.utils.AspectUtil.newHashSet;

/**
 * the Rx RateLimiter logic support for the spring AOP conditional on the presence of Rx classes on
 * the spring class loader
 */
public class RxJava3RateLimiterAspectExt implements RateLimiterAspectExt {

    private static final Logger logger = LoggerFactory.getLogger(RxJava3RateLimiterAspectExt.class);
    private final Set<Class> rxSupportedTypes = newHashSet(ObservableSource.class,
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
     * @param rateLimiter         the configured rateLimiter
     * @param methodName          the method name
     * @return the result object
     * @throws Throwable exception in case of faulty flow
     */
    @Override
    public Object handle(ProceedingJoinPoint proceedingJoinPoint, RateLimiter rateLimiter,
        String methodName) throws Throwable {
        RateLimiterOperator<?> rateLimiterOperator = RateLimiterOperator.of(rateLimiter);
        Object returnValue = proceedingJoinPoint.proceed();
        return executeRxJava3Aspect(rateLimiterOperator, returnValue);
    }

    @SuppressWarnings("unchecked")
    private Object executeRxJava3Aspect(RateLimiterOperator rateLimiterOperator,
        Object returnValue) {
        if (returnValue instanceof ObservableSource) {
            Observable<?> observable = (Observable) returnValue;
            return observable.compose(rateLimiterOperator);
        } else if (returnValue instanceof SingleSource) {
            Single<?> single = (Single) returnValue;
            return single.compose(rateLimiterOperator);
        } else if (returnValue instanceof CompletableSource) {
            Completable completable = (Completable) returnValue;
            return completable.compose(rateLimiterOperator);
        } else if (returnValue instanceof MaybeSource) {
            Maybe<?> maybe = (Maybe) returnValue;
            return maybe.compose(rateLimiterOperator);
        } else if (returnValue instanceof Flowable) {
            Flowable<?> flowable = (Flowable) returnValue;
            return flowable.compose(rateLimiterOperator);
        } else {
            logger.error("Unsupported type for Rate limiter RxJava3 {}",
                returnValue.getClass().getTypeName());
            throw new IllegalArgumentException(
                "Not Supported type for the Rate limiter in RxJava3 :" + returnValue.getClass()
                    .getName());
        }
    }
}
