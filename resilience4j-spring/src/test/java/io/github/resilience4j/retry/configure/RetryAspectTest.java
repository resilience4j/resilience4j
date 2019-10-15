package io.github.resilience4j.retry.configure;

import io.github.resilience4j.FlakyBehavior;
import io.github.resilience4j.TestApplication;
import io.github.resilience4j.TestDummyService;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {TestApplication.class, RetryAspectTest.Config.class})
public class RetryAspectTest {


	@Autowired
	@Qualifier("retryDummyService")
	TestDummyService testDummyService;

	@Autowired
	private RetryRegistry retryRegistry;

	@Test
	public void retryFutureAsyncTest() {
		FlakyBehavior flaky = mock(FlakyBehavior.class);

		when(flaky.execute())
				.thenThrow(new RuntimeException("error1"))
				.thenThrow(new RuntimeException("error2"))
				.thenReturn("success");

		testDummyService.asyncFuture(flaky);

		Retry retry = retryRegistry.retry(TestDummyService.BACKEND);

		await()
				.atMost(5, TimeUnit.SECONDS)
				.until(() -> retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt() == 1);


		Mockito.verify(flaky, times(3)).execute();
	}

	@Configuration
	static class Config {

		@Bean
		public RetryConfigurationProperties retryConfigurationProperties() {

			return new RetryConfigurationProperties() {
				{
					Arrays.asList(RuntimeException.class).toArray();
					InstanceProperties instanceProperties = new InstanceProperties();
					instanceProperties.setMaxRetryAttempts(3);

					Class<? extends Throwable>[] arr = new Class[1];
					arr[0] = RuntimeException.class;

					instanceProperties.setRetryExceptions(arr);
					getBackends().put(TestDummyService.BACKEND, instanceProperties);
				}
			};

		}
	}
}
