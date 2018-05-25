package io.github.resilience4j.service.test;


public interface DummyService {
    String BACKEND_A = "backendA";
    String BACKEND_B = "backendB";

    void doSomething(boolean throwException) throws Exception;
}
