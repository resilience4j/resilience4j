package io.github.resilience4j.bulkhead;

import io.micronaut.context.annotation.Factory;

import javax.inject.Singleton;

@Factory
public class BulkHeadRegistryFactory {

    @Singleton
    public BulkheadRegistry bulkheadRegistry() {
        return null;
    }
}
