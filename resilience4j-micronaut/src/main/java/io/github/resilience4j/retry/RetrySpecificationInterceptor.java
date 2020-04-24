package io.github.resilience4j.retry;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;

import javax.inject.Singleton;

@Singleton
@Internal
@Requires(classes = RetryRegistry.class)
public class RetrySpecificationInterceptor {
}
