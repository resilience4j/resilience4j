package io.github.resilience4j.service.test;


import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.github.resilience4j.retry.annotation.AsyncRetry;
import io.github.resilience4j.retry.annotation.Retry;

@Component
public class RetryDummyServiceImpl implements RetryDummyService {

	@Retry(name = RetryDummyService.RETRY_BACKEND_A)
	@Override
	public void doSomething(boolean throwBackendTrouble) throws IOException {
		if (throwBackendTrouble) {
			throw new IOException("Test Message");
		}
	}

	@AsyncRetry(name = RetryDummyService.RETRY_BACKEND_B)
	@Override
	public CompletionStage<String> doSomethingAsync(boolean throwException) throws IOException {
		if (throwException) {
			throw new IOException("Test Message");
		} else {
			return CompletableFuture.supplyAsync(() -> "test");
		}

	}
}
