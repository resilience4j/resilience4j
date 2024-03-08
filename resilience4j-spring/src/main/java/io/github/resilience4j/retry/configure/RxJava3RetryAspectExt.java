package io.github.resilience4j.retry.configure;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.rxjava3.retry.transformer.RetryTransformer;
import io.reactivex.rxjava3.core.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static io.github.resilience4j.utils.AspectUtil.newHashSet;

/**
 * the Rx Retry logic support for the spring AOP conditional on the presence of Rx classes on the
 * spring class loader
 */
public class RxJava3RetryAspectExt implements RetryAspectExt {

    private static final Logger logger = LoggerFactory.getLogger(RxJava3RetryAspectExt.class);
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
     * @param retry               the configured Retry
     * @param methodName          the method name
     * @return the result object
     * @throws Throwable exception in case of faulty flow
     */
    @Override
    public Object handle(ProceedingJoinPoint proceedingJoinPoint, Retry retry, String methodName)
        throws Throwable {
        RetryTransformer<?> retryTransformer = RetryTransformer.of(retry);
        Object returnValue = proceedingJoinPoint.proceed();
        return executeRxJava3Aspect(retryTransformer, returnValue);
    }

    @SuppressWarnings("unchecked")
    private Object executeRxJava3Aspect(RetryTransformer retryTransformer, Object returnValue) {
        if (returnValue instanceof ObservableSource) {
            Observable<?> observable = (Observable<?>) returnValue;
            return observable.compose(retryTransformer);
        } else if (returnValue instanceof SingleSource) {
            Single<?> single = (Single) returnValue;
            return single.compose(retryTransformer);
        } else if (returnValue instanceof CompletableSource) {
            Completable completable = (Completable) returnValue;
            return completable.compose(retryTransformer);
        } else if (returnValue instanceof MaybeSource) {
            Maybe<?> maybe = (Maybe) returnValue;
            return maybe.compose(retryTransformer);
        } else if (returnValue instanceof Flowable) {
            Flowable<?> flowable = (Flowable) returnValue;
            return flowable.compose(retryTransformer);
        } else {
            logger.error("Unsupported type for retry RxJava3 {}",
                returnValue.getClass().getTypeName());
            throw new IllegalArgumentException(
                "Not Supported type for the Retry in RxJava3 :" + returnValue.getClass().getName());
        }
    }
}
