package io.github.resilience4j.spring6.micrometer.configure;

import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.rxjava3.micrometer.transformer.TimerTransformer;
import io.github.resilience4j.spring6.timelimiter.configure.IllegalReturnTypeException;
import io.reactivex.rxjava3.core.*;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.Set;

import static io.github.resilience4j.spring6.utils.AspectUtil.newHashSet;

public class RxJava3TimerAspectExt implements TimerAspectExt {

    private final Set<Class<?>> rxSupportedTypes = newHashSet(ObservableSource.class, SingleSource.class, CompletableSource.class, MaybeSource.class, Flowable.class);

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
     * @param timer               the configured timer
     * @param methodName          the method name
     * @return the result object
     * @throws Throwable exception in case of faulty flow
     */
    @Override
    public Object handle(ProceedingJoinPoint proceedingJoinPoint, Timer timer, String methodName) throws Throwable {
        TimerTransformer<?> timerTransformer = TimerTransformer.of(timer);
        Object returnValue = proceedingJoinPoint.proceed();
        return executeRxJava3Aspect(timerTransformer, returnValue, methodName);
    }

    @SuppressWarnings("unchecked")
    private static Object executeRxJava3Aspect(TimerTransformer timerTransformer, Object returnValue, String methodName) {
        if (returnValue instanceof ObservableSource) {
            Observable<?> observable = (Observable<?>) returnValue;
            return observable.compose(timerTransformer);
        } else if (returnValue instanceof SingleSource) {
            Single<?> single = (Single<?>) returnValue;
            return single.compose(timerTransformer);
        } else if (returnValue instanceof CompletableSource) {
            Completable completable = (Completable) returnValue;
            return completable.compose(timerTransformer);
        } else if (returnValue instanceof MaybeSource) {
            Maybe<?> maybe = (Maybe<?>) returnValue;
            return maybe.compose(timerTransformer);
        } else if (returnValue instanceof Flowable) {
            Flowable<?> flowable = (Flowable<?>) returnValue;
            return flowable.compose(timerTransformer);
        } else {
            throw new IllegalReturnTypeException(returnValue.getClass(), methodName, "RxJava3 expects Flowable/Single/...");
        }
    }
}
