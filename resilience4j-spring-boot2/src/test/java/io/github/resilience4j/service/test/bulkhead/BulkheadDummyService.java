package io.github.resilience4j.service.test.bulkhead;

public interface BulkheadDummyService {
    String BACKEND = "backendA";

    void doSomething();
}
