package io.github.resilience4j.springboot3.service.test;

import java.io.IOException;

public interface CbOnlyReproService {

    void doSomething(boolean throwBackendTrouble) throws IOException;
}
