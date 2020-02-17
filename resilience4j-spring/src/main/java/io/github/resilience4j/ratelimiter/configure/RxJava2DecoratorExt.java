/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.ratelimiter.configure;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.operator.RateLimiterOperator;
import io.reactivex.*;
import io.vavr.CheckedFunction0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static io.github.resilience4j.utils.AspectUtil.newHashSet;

/**
 * Decorator support for return types which belong to RxJava2
 */
public class RxJava2DecoratorExt implements RateLimiterDecoratorExt {

    private static final Logger logger = LoggerFactory.getLogger(RxJava2DecoratorExt.class);
    private final Set<Class> rxSupportedTypes = newHashSet(ObservableSource.class,
        SingleSource.class, CompletableSource.class, MaybeSource.class, Flowable.class);

    /**
     * @param returnType the AOP method return type class
     * @return boolean if the return type belongs to RxJava2
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean canDecorateReturnType(Class returnType) {
        return rxSupportedTypes.stream()
            .anyMatch(classType -> classType.isAssignableFrom(returnType));
    }

    /**
     * Decorate a function with a RateLimiter.
     *
     * @param rateLimiter         the  rateLimiter
     * @param function The function

     * @return the result object
     */
    @Override
    public CheckedFunction0<Object> decorate(RateLimiter rateLimiter, CheckedFunction0<Object> function) {
        return () -> {
            RateLimiterOperator<?> rateLimiterOperator = RateLimiterOperator.of(rateLimiter);
            Object returnValue = function.apply();
            return executeRxJava2Aspect(rateLimiterOperator, returnValue);
        };
    }

    @SuppressWarnings("unchecked")
    private Object executeRxJava2Aspect(RateLimiterOperator rateLimiterOperator,
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
            logger.error("Unsupported type for Rate limiter RxJava2 {}",
                returnValue.getClass().getTypeName());
            throw new IllegalArgumentException(
                "Not Supported type for the Rate limiter in RxJava2 :" + returnValue.getClass()
                    .getName());
        }
    }
}
