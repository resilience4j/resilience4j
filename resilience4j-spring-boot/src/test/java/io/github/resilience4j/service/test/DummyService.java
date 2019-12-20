package io.github.resilience4j.service.test;


import java.io.IOException;

public interface DummyService {

    String BACKEND = "backendA";
    String BACKEND_C = "backendC";

    void doSomething(boolean throwException) throws IOException;
}
