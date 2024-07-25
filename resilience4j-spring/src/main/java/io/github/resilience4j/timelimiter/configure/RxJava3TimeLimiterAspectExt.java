package io.github.resilience4j.timelimiter.configure;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.rxjava3.timelimiter.transformer.TimeLimiterTransformer;
import io.reactivex.rxjava3.core.*;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.Set;

import static io.github.resilience4j.utils.AspectUtil.newHashSet;

public class RxJava3TimeLimiterAspectExt implements TimeLimiterAspectExt {

    private final Set<Class<?>> rxSupportedTypes = newHashSet(ObservableSource.class,
        SingleSource.class, CompletableSource.class, MaybeSource.class, Flowable.class);

    /**
     * @param returnType the AOP method return type class
     * @return boolean if the method has Rx java 3 rerun type
     */
    @Override
    public boolean canHandleReturnType(Class<?> returnType) {
        return rxSupportedTypes.stream().anyMatch(classType -> classType.isAssignableFrom(returnType));
    }

    /**
     * @param proceedingJoinPoint Spring AOP proceedingJoinPoint
     * @param timeLimiter         the configured timeLimiter
     * @param methodName          the method name
     * @return the result object
     * @throws Throwable exception in case of faulty flow
     */
    @Override
    public Object handle(ProceedingJoinPoint proceedingJoinPoint, TimeLimiter timeLimiter, String methodName)
        throws Throwable {
        TimeLimiterTransformer<?> timeLimiterTransformer = TimeLimiterTransformer.of(timeLimiter);
        Object returnValue = proceedingJoinPoint.proceed();
        return executeRxJava3Aspect(timeLimiterTransformer, returnValue, methodName);
    }

    @SuppressWarnings("unchecked")
    private static Object executeRxJava3Aspect(TimeLimiterTransformer timeLimiterTransformer,
        Object returnValue, String methodName) {
        if (returnValue instanceof ObservableSource) {
            Observable<?> observable = (Observable<?>) returnValue;
            return observable.compose(timeLimiterTransformer);
        } else if (returnValue instanceof SingleSource) {
            Single<?> single = (Single<?>) returnValue;
            return single.compose(timeLimiterTransformer);
        } else if (returnValue instanceof CompletableSource) {
            Completable completable = (Completable) returnValue;
            return completable.compose(timeLimiterTransformer);
        } else if (returnValue instanceof MaybeSource) {
            Maybe<?> maybe = (Maybe<?>) returnValue;
            return maybe.compose(timeLimiterTransformer);
        } else if (returnValue instanceof Flowable) {
            Flowable<?> flowable = (Flowable<?>) returnValue;
            return flowable.compose(timeLimiterTransformer);
        } else {
            throw new IllegalReturnTypeException(returnValue.getClass(), methodName,
                "RxJava3 expects Flowable/Single/...");
        }
    }
}
