package io.github.resilience4j.springboot.nativeimage.configuration;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

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

        hints.reflection().registerType(io.github.resilience4j.spring6.fallback.FallbackExecutor.class,
            builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
        hints.reflection().registerType(io.github.resilience4j.spring6.fallback.FallbackMethod.class,
            builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
        hints.reflection().registerType(io.github.resilience4j.spring6.utils.AnnotationExtractor.class,
            builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
        hints.reflection().registerType(org.springframework.context.expression.MethodBasedEvaluationContext.class,
            builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
    }
}
