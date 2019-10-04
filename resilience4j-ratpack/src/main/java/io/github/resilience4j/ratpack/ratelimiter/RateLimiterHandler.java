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
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import static io.github.resilience4j.ratelimiter.RequestNotPermitted.createRequestNotPermitted;

public class RateLimiterHandler implements Handler {

    private final RateLimiter rateLimiter;

    public RateLimiterHandler(RateLimiterRegistry rateLimiterRegistry, String rateLimiterName) {
        this.rateLimiter = rateLimiterRegistry.rateLimiter(rateLimiterName);
    }

    public RateLimiterHandler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        boolean permission = rateLimiter.acquirePermission();
        if (Thread.interrupted()) {
            throw new IllegalStateException("Thread was interrupted during permission wait");
        }
        if (!permission) {
            Throwable t = createRequestNotPermitted(rateLimiter);
            ctx.error(t);
        } else {
            ctx.next();
        }
    }

}
