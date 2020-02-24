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
package io.github.resilience4j.retry.configure;

import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.vavr.CheckedFunction0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * the Reactor Retry logic support for the spring AOP Conditional on Reactor class existence on
 * spring class loader
 */
public class ReactorDecoratorExt implements RetryDecoratorExt {

    private static final Logger logger = LoggerFactory.getLogger(ReactorDecoratorExt.class);

    /**
     * @param returnType the AOP method return type class
     * @return boolean if the method has Reactor return type
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean canDecorateReturnType(Class returnType) {
        return (Flux.class.isAssignableFrom(returnType)) || (Mono.class
            .isAssignableFrom(returnType));
    }

    /**
     * Decorate a function with a RateLimiter.
     *
     * @param retry         the retry
     * @param function the function
     * @return the result object
     */
    @Override
    public CheckedFunction0<Object> decorate(Retry retry, CheckedFunction0<Object> function) {
        return () -> {
            Object returnValue = function.apply();
            if (Flux.class.isAssignableFrom(returnValue.getClass())) {
                Flux<?> fluxReturnValue = (Flux<?>) returnValue;
                return fluxReturnValue.compose(RetryOperator.of(retry));
            } else if (Mono.class.isAssignableFrom(returnValue.getClass())) {
                Mono<?> monoReturnValue = (Mono<?>) returnValue;
                return monoReturnValue.compose(RetryOperator.of(retry));
            } else {
                logger.error("Unsupported type for Reactor retry {}",
                    returnValue.getClass().getTypeName());
                throw new IllegalArgumentException(
                    "Not Supported type for the retry in Reactor :" + returnValue.getClass().getName());

            }
        };
    }
}
