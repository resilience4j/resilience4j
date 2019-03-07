package io.github.resilience4j.service.test;


import java.io.IOException;

public interface RetryDummyService {
	String BACKEND = "retryBackendA";

	void doSomething(boolean throwException) throws IOException;
}
