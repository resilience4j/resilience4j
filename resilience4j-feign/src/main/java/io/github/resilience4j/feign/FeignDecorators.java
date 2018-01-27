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

import java.util.ArrayList;
import java.util.List;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.vavr.CheckedFunction1;

/**
 * Builder to help build stacked decorators.
 */
public class FeignDecorators implements FeignDecorator {

    private final List<FeignDecorator> decorators;

    private FeignDecorators(List<FeignDecorator> decorators) {
        this.decorators = decorators;
    }

    @Override
    public CheckedFunction1<Object[], Object> decorate(CheckedFunction1<Object[], Object> fn) {
        CheckedFunction1<Object[], Object> decoratedFn = fn;
        for (final FeignDecorator decorator : decorators) {
            decoratedFn = decorator.decorate(decoratedFn);
        }
        return decoratedFn;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<FeignDecorator> decorators = new ArrayList<>();

        public Builder withCircuitBreaker(CircuitBreaker circuitBreaker) {
            decorators.add(fn -> CircuitBreaker.decorateCheckedFunction(circuitBreaker, fn));
            return this;
        }

        public Builder withRateLimiter(RateLimiter rateLimiter) {
            decorators.add(fn -> RateLimiter.decorateCheckedFunction(rateLimiter, fn));
            return this;
        }

        public FeignDecorators build() {
            return new FeignDecorators(decorators);
        }

    }

}
