/*
 * Copyright 2017 Dan Maas
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
package io.github.resilience4j.ratpack.ratelimiter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratpack.internal.AbstractTransformer;
import ratpack.exec.Downstream;
import ratpack.exec.Upstream;
import ratpack.func.Function;

import static io.github.resilience4j.ratelimiter.RequestNotPermitted.getRequestNotPermitted;

public class RateLimiterTransformer<T> extends AbstractTransformer<T> {

    private final RateLimiter rateLimiter;

    private RateLimiterTransformer(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * Create a new transformer that can be applied to the {@link ratpack.exec.Promise#transform(Function)} method.
     * The Promised value will pass through the rateLimiter, potentially causing it to rateLimiter on error.
     *
     * @param rateLimiter the rateLimiter to use
     * @param <T>         the type of object
     * @return the transformer
     */
    public static <T> RateLimiterTransformer<T> of(RateLimiter rateLimiter) {
        return new RateLimiterTransformer<>(rateLimiter);
    }

    /**
     * Set a recovery function that will execute when the rateLimiter limit is exceeded.
     *
     * @param recoverer the recovery function
     * @return the transformer
     */
    public RateLimiterTransformer<T> recover(Function<Throwable, ? extends T> recoverer) {
        this.recoverer = recoverer;
        return this;
    }

    @Override
    public Upstream<T> apply(Upstream<? extends T> upstream) throws Exception {
        return down -> {
            boolean permission = rateLimiter.acquirePermission();
            if (Thread.interrupted()) {
                throw new IllegalStateException("Thread was interrupted during permission wait");
            }
            if (!permission) {
                Throwable t = getRequestNotPermitted(rateLimiter);
                if (recoverer != null) {
                    down.success(recoverer.apply(t));
                } else {
                    down.error(t);
                }
            } else {
                upstream.connect(new Downstream<T>() {

                    @Override
                    public void success(T value) {
                        down.success(value);
                    }

                    @Override
                    public void error(Throwable throwable) {
                        down.error(throwable);
                    }

                    @Override
                    public void complete() {
                        down.complete();
                    }
                });
            }
        };
    }

}
