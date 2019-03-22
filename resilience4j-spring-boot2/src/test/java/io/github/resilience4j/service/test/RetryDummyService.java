package io.github.resilience4j.service.test;


import java.io.IOException;
import java.util.concurrent.CompletionStage;

public interface RetryDummyService {
	String BACKEND = "retryBackendA";

	void doSomething(boolean throwException) throws IOException;

	CompletionStage<String> doSomethingAsync(boolean throwException) throws IOException;
}
