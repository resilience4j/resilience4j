package io.github.resilience4j.circuitbreaker.test;


import java.io.IOException;

public interface DummyService {
    String BACKEND = "backendA";

    void doSomething(boolean throwException) throws IOException;
}
