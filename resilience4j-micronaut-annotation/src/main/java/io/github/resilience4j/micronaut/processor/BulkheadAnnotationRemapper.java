package io.github.resilience4j.micronaut.processor;

import io.micronaut.inject.annotation.PackageRenameRemapper;

/**
 * Allows using either resilience4j annotations or the Micronaut versions.
 */
public final class BulkheadAnnotationRemapper implements PackageRenameRemapper {

    @Override
    public String getTargetPackage() {
        return "io.github.resilience4j.micronaut.annotation";
    }

    @Override
    public String getPackageName() {
        return "io.github.resilience4j.bulkhead.annotation";
    }
}
