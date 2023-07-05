/*
 * Copyright 2023 Mariusz Kopylec
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.resilience4j.spring6.micrometer.configure;

import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.transformer.TimerTransformer;
import io.github.resilience4j.spring6.timelimiter.configure.IllegalReturnTypeException;
import io.reactivex.*;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.Set;

import static io.github.resilience4j.spring6.utils.AspectUtil.newHashSet;

public class RxJava2TimerAspectExt implements TimerAspectExt {

    private final Set<Class<?>> rxSupportedTypes = newHashSet(ObservableSource.class,
            SingleSource.class, CompletableSource.class, MaybeSource.class, Flowable.class);

    /**
     * @param returnType the AOP method return type class
     * @return boolean if the method has Rx java 2 rerun type
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
        return executeRxJava2Aspect(timerTransformer, returnValue, methodName);
    }

    @SuppressWarnings("unchecked")
    private static Object executeRxJava2Aspect(TimerTransformer timerTransformer, Object returnValue, String methodName) {
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
            throw new IllegalReturnTypeException(returnValue.getClass(), methodName, "RxJava2 expects Flowable/Single/...");
        }
    }
}
