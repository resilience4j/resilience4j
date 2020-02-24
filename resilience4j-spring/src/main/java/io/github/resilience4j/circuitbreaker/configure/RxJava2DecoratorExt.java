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
package io.github.resilience4j.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
import io.reactivex.*;
import io.vavr.CheckedFunction0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static io.github.resilience4j.utils.AspectUtil.newHashSet;

/**
 * the Rx circuit breaker logic support for the spring AOP conditional on the presence of Rx classes
 * on the spring class loader
 */
public class RxJava2DecoratorExt implements CircuitBreakerDecoratorExt {

    private static final Logger logger = LoggerFactory
        .getLogger(RxJava2DecoratorExt.class);
    private final Set<Class> rxSupportedTypes = newHashSet(ObservableSource.class,
        SingleSource.class, CompletableSource.class, MaybeSource.class, Flowable.class);

    /**
     * @param returnType the AOP method return type class
     * @return boolean if the method has Rx java 2 rerun type
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean canDecorateReturnType(Class returnType) {
        return rxSupportedTypes.stream()
            .anyMatch(classType -> classType.isAssignableFrom(returnType));
    }

    /**
     * Decorate a function with a CircuitBreaker.
     *
     * @param circuitBreaker         the  CircuitBreaker
     * @param function The function

     * @return the result object
     */
    @Override
    public CheckedFunction0<Object> decorate(CircuitBreaker circuitBreaker, CheckedFunction0<Object> function) {
        return () -> {
            CircuitBreakerOperator circuitBreakerOperator = CircuitBreakerOperator.of(circuitBreaker);
            Object returnValue = function.apply();

            return executeRxJava2Aspect(circuitBreakerOperator, returnValue);
        };

    }

    @SuppressWarnings("unchecked")
    private Object executeRxJava2Aspect(CircuitBreakerOperator circuitBreakerOperator,
        Object returnValue) {
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
            logger.error("Unsupported type for RxJava2 circuit breaker {}",
                returnValue.getClass().getTypeName());
            throw new IllegalArgumentException(
                "Not Supported type for the circuit breaker in RxJava2:" + returnValue.getClass()
                    .getName());

        }
    }
}
