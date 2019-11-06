package io.github.resilience4j.service.test;

public interface BulkheadDummyService {

    String BACKEND = "backendA";

    void doSomething();
}
