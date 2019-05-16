/*
 * Copyright 2019 Robert Winkler
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
package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.ResilienceBaseObserver;
import io.github.resilience4j.ratelimiter.RateLimiter;

import static java.util.Objects.requireNonNull;

/**
 * A base RateLimiter observer.
 *
 */
abstract class BaseRateLimiterObserver extends ResilienceBaseObserver {

    private final RateLimiter rateLimiter;

    BaseRateLimiterObserver(RateLimiter rateLimiter) {
        this.rateLimiter = requireNonNull(rateLimiter);
    }

    protected void onSuccess() {
    }

    protected void onError(Throwable t) {
    }

    @Override
    public void hookOnCancel() {
    }
}
