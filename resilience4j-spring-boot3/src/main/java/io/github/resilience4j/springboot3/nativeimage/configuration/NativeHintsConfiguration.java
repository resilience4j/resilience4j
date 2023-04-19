package io.github.resilience4j.springboot3.nativeimage.configuration;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(NativeHintsConfiguration.class)
public class NativeHintsConfiguration implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(io.github.resilience4j.spring6.bulkhead.configure.BulkheadAspect.class,
            builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));

        hints.reflection().registerType(io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerAspect.class,
            builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));

        hints.reflection().registerType(io.github.resilience4j.spring6.ratelimiter.configure.RateLimiterAspect.class,
            builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));

        hints.reflection().registerType(io.github.resilience4j.spring6.retry.configure.RetryAspect.class,
            builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));

        hints.reflection().registerType(io.github.resilience4j.spring6.timelimiter.configure.TimeLimiterAspect.class,
            builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
    }
}