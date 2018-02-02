/*
 *
 * Copyright 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.feign;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.vavr.CheckedFunction1;

/**
 * Builder to help build stacked decorators. The order in which decorators are applied correspond to
 * the order in which they are declared. For example, calling
 * {@link FeignDecorators.Builder#withCircuitBreaker(CircuitBreaker)} before
 * {@link FeignDecorators.Builder#withFallback(Object)} would mean that the fallback is called when
 * the CircuitBreaker is open. However, reversing the order would mean that although the fallback
 * would still be called when the HTTP request fails, it would no longer be called when the
 * CircuitBreaker is open. <br>
 * So be wary of this when designing your "resilience" strategy.
 */
public class FeignDecorators implements FeignDecorator {

    private final List<FeignDecorator> decorators;

    private FeignDecorators(List<FeignDecorator> decorators) {
        this.decorators = decorators;
    }

    @Override
    public CheckedFunction1<Object[], Object> decorate(CheckedFunction1<Object[], Object> fn,
            Method method, MethodHandler methodHandler, Target<?> target) {
        CheckedFunction1<Object[], Object> decoratedFn = fn;
        for (final FeignDecorator decorator : decorators) {
            decoratedFn = decorator.decorate(decoratedFn, method, methodHandler, target);
        }
        return decoratedFn;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<FeignDecorator> decorators = new ArrayList<>();

        public Builder withCircuitBreaker(CircuitBreaker circuitBreaker) {
            decorators.add((fn, m, mh, t) -> CircuitBreaker.decorateCheckedFunction(circuitBreaker, fn));
            return this;
        }

        public Builder withRateLimiter(RateLimiter rateLimiter) {
            decorators.add((fn, m, mh, t) -> RateLimiter.decorateCheckedFunction(rateLimiter, fn));
            return this;
        }

        public Builder withFallback(Object fallback) {
            decorators.add((fn, m, mh, t) -> {
                return args -> {
                    try {
                        return fn.apply(args);
                    } catch (final Throwable throwable) {
                        if (fallback != null) {
                            return fallback.getClass().getMethod(m.getName(), m.getParameterTypes()).invoke(fallback, args);
                        }
                        throw throwable;
                    }
                };
            });
            return this;
        }

        public FeignDecorators build() {
            return new FeignDecorators(decorators);
        }

    }

}
