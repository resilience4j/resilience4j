package io.github.resilience4j.micronaut.processor;

import io.micronaut.inject.annotation.PackageRenameRemapper;

public class CircuitBreakerAnnotationRemapper implements PackageRenameRemapper {

    @Override
    public String getTargetPackage() {
        return "io.github.resilience4j.micronaut.annotation";
    }

    @Override
    public String getPackageName() {
        return "io.github.resilience4j.ratelimiter.annotation";
    }
}
