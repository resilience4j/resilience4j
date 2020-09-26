package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

interface InMemoryFactory<C extends RateLimiterConfig> {

    RateLimiter create(String name, C configuration, io.vavr.collection.Map<String,String> allTags);

    static InMemoryFactory<RateLimiterConfig> atomicFactory() {
        return RateLimiter::of;
    }

}
