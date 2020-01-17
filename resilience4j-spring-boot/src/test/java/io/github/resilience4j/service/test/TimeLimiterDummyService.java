package io.github.resilience4j.service.test;

public interface TimeLimiterDummyService {
    String BACKEND = "backend";

    void doSomething(boolean timeout) throws Exception;

}
