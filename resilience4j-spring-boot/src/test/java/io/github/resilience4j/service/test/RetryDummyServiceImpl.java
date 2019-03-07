package io.github.resilience4j.service.test;


import java.io.IOException;

import org.springframework.stereotype.Component;

import io.github.resilience4j.retry.annotation.Retry;

@Retry(name = RetryDummyService.BACKEND)
@Component
public class RetryDummyServiceImpl implements RetryDummyService {
	@Override
	public void doSomething(boolean throwBackendTrouble) throws IOException {
		if (throwBackendTrouble) {
			throw new IOException("Test Message");
		}
	}
}
