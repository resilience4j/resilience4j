package io.github.resilience4j.bulkhead.internal;

import io.github.resilience4j.core.NamingThreadFactory;

/**
 * Creates threads using "bulkhead-$name-%d" pattern for naming. Is based on {@link
 * java.util.concurrent.Executors.DefaultThreadFactory}.
 */
class BulkheadNamingThreadFactory extends NamingThreadFactory {

    BulkheadNamingThreadFactory(String name) {
        super(String.join("-", "bulkhead", name));
    }
}
