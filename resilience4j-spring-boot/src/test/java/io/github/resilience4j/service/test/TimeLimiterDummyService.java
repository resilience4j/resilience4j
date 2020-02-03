package io.github.resilience4j.service.test;

import java.util.concurrent.CompletionStage;

public interface TimeLimiterDummyService {
    String BACKEND = "backend";

    CompletionStage<String> doSomething(boolean timeout) throws Exception;

}
