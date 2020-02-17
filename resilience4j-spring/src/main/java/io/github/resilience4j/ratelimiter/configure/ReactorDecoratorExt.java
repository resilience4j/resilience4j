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
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.vavr.CheckedFunction0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Decorator support for return types which belong to Spring Reactor
 */
public class ReactorDecoratorExt implements RateLimiterDecoratorExt {

    private static final Logger logger = LoggerFactory.getLogger(ReactorDecoratorExt.class);

    /**
     * @param returnType the AOP method return type class
     * @return boolean if the return type belongs to Spring Reactor
     */
    @Override
    public boolean canDecorateReturnType(Class returnType) {
        return (Flux.class.isAssignableFrom(returnType)) || (Mono.class
            .isAssignableFrom(returnType));
    }

    /**
     * Decorate a function with a RateLimiter.
     *
     * @param rateLimiter         the rateLimiter
     * @param function the function
     * @return the result object
     */
    @Override
    public CheckedFunction0<Object> decorate(RateLimiter rateLimiter, CheckedFunction0<Object> function) {
        return () -> {
            Object returnValue = function.apply();
            if (Flux.class.isAssignableFrom(returnValue.getClass())) {
                Flux<?> fluxReturnValue = (Flux<?>) returnValue;
                return fluxReturnValue.compose(RateLimiterOperator.of(rateLimiter));
            } else if (Mono.class.isAssignableFrom(returnValue.getClass())) {
                Mono<?> monoReturnValue = (Mono<?>) returnValue;
                return monoReturnValue.compose(RateLimiterOperator.of(rateLimiter));
            } else {
                logger.error("Unsupported type for Reactor rateLimiter {}",
                    returnValue.getClass().getTypeName());
                throw new IllegalArgumentException(
                    "Not Supported type for the rateLimiter in Reactor :" + returnValue.getClass()
                        .getName());

            }
        };
    }
}
