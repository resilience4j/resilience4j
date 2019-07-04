package io.github.resilience4j.bulkhead.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.xml.ws.WebServiceException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.internal.config.MovingAverageConfig;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;


public class AdaptiveBulkheadTest {
	private AdaptiveBulkhead default_bulkhead;
	private AdaptiveBulkheadConfig<MovingAverageConfig> config;
	private HelloWorldService helloWorldService;

	@Before
	public void setUp() {
		helloWorldService = Mockito.mock(HelloWorldService.class);

		config = AdaptiveBulkheadConfig.<MovingAverageConfig>builder().config(MovingAverageConfig.builder().maxAcceptableRequestLatency(0.2)
				.desirableAverageThroughput(2)
				.desirableOperationLatency(0.1)
				.build()).build();
		default_bulkhead = AdaptiveBulkhead.of("test", config);
	}

	@Test
	public void shouldReturnTheCorrectName() {
		assertThat(default_bulkhead.getName()).isEqualTo("test");
	}

	@Test
	public void testToString() {

		// when
		String result = default_bulkhead.toString();

		// then
		assertThat(result).isEqualTo("AdaptiveBulkhead 'test'");
	}

	@Test
	public void testCreateWithNullConfig() {
		// when
		assertThatThrownBy(() -> AdaptiveBulkhead.of("test", () -> null)).isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
	}

	@Test
	public void testCreateWithDefaults() {

		// when
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.ofDefaults("test");

		// then
		assertThat(bulkhead).isNotNull();
		assertThat(bulkhead.getBulkheadConfig()).isNotNull();
		assertThat(((AdaptiveBulkheadConfig<MovingAverageConfig>) bulkhead.getBulkheadConfig()).getConfiguration().getWindowForAdaptation()).isEqualTo(50);
	}

	@Test
	public void shouldDecorateSupplierAndReturnWithSuccess() {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

		// When
		Supplier<String> supplier = AdaptiveBulkhead.decorateSupplier(bulkhead, helloWorldService::returnHelloWorld);

		// Then
		assertThat(supplier.get()).isEqualTo("Hello world");
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
	}

	@Test
	public void shouldExecuteSupplierAndReturnWithSuccess() {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

		// When
		String result = bulkhead.executeSupplier(helloWorldService::returnHelloWorld);

		// Then
		assertThat(result).isEqualTo("Hello world");
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
	}

	@Test
	public void shouldDecorateSupplierAndReturnWithException() {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM!"));

		// When
		Supplier<String> supplier = AdaptiveBulkhead.decorateSupplier(bulkhead, helloWorldService::returnHelloWorld);
		Try<String> result = Try.of(supplier::get);

		//Then
		assertThat(result.isFailure()).isTrue();
		assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
		BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
	}

	@Test
	public void shouldDecorateCheckedSupplierAndReturnWithSuccess() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

		// When
		CheckedFunction0<String> checkedSupplier = AdaptiveBulkhead.decorateCheckedSupplier(bulkhead, helloWorldService::returnHelloWorldWithException);

		// Then
		assertThat(checkedSupplier.apply()).isEqualTo("Hello world");
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
	}

	@Test
	public void shouldDecorateCheckedSupplierAndReturnWithException() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willThrow(new RuntimeException("BAM!"));

		// When
		CheckedFunction0<String> checkedSupplier = AdaptiveBulkhead.decorateCheckedSupplier(bulkhead, helloWorldService::returnHelloWorldWithException);
		Try<String> result = Try.of(checkedSupplier);

		// Then
		assertThat(result.isFailure()).isTrue();
		assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
		BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
	}

	@Test
	public void shouldDecorateCallableAndReturnWithSuccess() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

		// When
		Callable<String> callable = AdaptiveBulkhead.decorateCallable(bulkhead, helloWorldService::returnHelloWorldWithException);

		// Then
		assertThat(callable.call()).isEqualTo("Hello world");
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
	}

	@Test
	public void shouldExecuteCallableAndReturnWithSuccess() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

		// When
		String result = bulkhead.executeCallable(helloWorldService::returnHelloWorldWithException);

		// Then
		assertThat(result).isEqualTo("Hello world");
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
	}

	@Test
	public void shouldDecorateCallableAndReturnWithException() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willThrow(new RuntimeException("BAM!"));

		// When
		Callable<String> callable = AdaptiveBulkhead.decorateCallable(bulkhead, helloWorldService::returnHelloWorldWithException);
		Try<String> result = Try.of(callable::call);

		// Then
		assertThat(result.isFailure()).isTrue();
		assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
		BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
	}

	@Test
	public void shouldDecorateCheckedRunnableAndReturnWithSuccess() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

		// When
		AdaptiveBulkhead.decorateCheckedRunnable(bulkhead, helloWorldService::sayHelloWorldWithException)
				.run();

		// Then
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorldWithException();
	}

	@Test
	public void shouldDecorateCheckedRunnableAndReturnWithException() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

		// When
		CheckedRunnable checkedRunnable = AdaptiveBulkhead.decorateCheckedRunnable(bulkhead, () -> {
			throw new RuntimeException("BAM!");
		});
		Try<Void> result = Try.run(checkedRunnable);

		// Then
		assertThat(result.isFailure()).isTrue();
		assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
	}

	@Test
	public void shouldDecorateRunnableAndReturnWithSuccess() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

		//When
		AdaptiveBulkhead.decorateRunnable(bulkhead, helloWorldService::sayHelloWorld)
				.run();

		//Then
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorld();
	}

	@Test
	public void shouldExecuteRunnableAndReturnWithSuccess() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

		// When
		bulkhead.executeRunnable(helloWorldService::sayHelloWorld);

		// Then
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorld();
	}

	@Test
	public void shouldDecorateRunnableAndReturnWithException() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

		// When
		Runnable runnable = AdaptiveBulkhead.decorateRunnable(bulkhead, () -> {
			throw new RuntimeException("BAM!");
		});
		Try<Void> result = Try.run(runnable::run);

		//Then
		assertThat(result.isFailure()).isTrue();
		assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
	}

	@Test
	public void shouldDecorateConsumerAndReturnWithSuccess() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

		// When
		AdaptiveBulkhead.decorateConsumer(bulkhead, helloWorldService::sayHelloWorldWithName)
				.accept("Tom");

		// Then
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorldWithName("Tom");
	}

	@Test
	public void shouldDecorateConsumerAndReturnWithException() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

		// When
		Consumer<String> consumer = AdaptiveBulkhead.decorateConsumer(bulkhead, (value) -> {
			throw new RuntimeException("BAM!");
		});
		Try<Void> result = Try.run(() -> consumer.accept("Tom"));

		// Then
		assertThat(result.isFailure()).isTrue();
		assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
	}

	@Test
	public void shouldDecorateCheckedConsumerAndReturnWithSuccess() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

		// When
		AdaptiveBulkhead.decorateCheckedConsumer(bulkhead, helloWorldService::sayHelloWorldWithNameWithException)
				.accept("Tom");

		// Then
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorldWithNameWithException("Tom");
	}

	@Test
	public void shouldDecorateCheckedConsumerAndReturnWithException() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

		// When
		CheckedConsumer<String> checkedConsumer = AdaptiveBulkhead.decorateCheckedConsumer(bulkhead, (value) -> {
			throw new RuntimeException("BAM!");
		});
		Try<Void> result = Try.run(() -> checkedConsumer.accept("Tom"));

		// Then
		assertThat(result.isFailure()).isTrue();
		assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
	}

	@Test
	public void shouldDecorateFunctionAndReturnWithSuccess() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorldWithName("Tom")).willReturn("Hello world Tom");

		// When
		Function<String, String> function = AdaptiveBulkhead.decorateFunction(bulkhead, helloWorldService::returnHelloWorldWithName);

		// Then
		assertThat(function.apply("Tom")).isEqualTo("Hello world Tom");
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithName("Tom");
	}

	@Test
	public void shouldDecorateFunctionAndReturnWithException() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorldWithName("Tom")).willThrow(new RuntimeException("BAM!"));

		// When
		Function<String, String> function = AdaptiveBulkhead.decorateFunction(bulkhead, helloWorldService::returnHelloWorldWithName);
		Try<String> result = Try.of(() -> function.apply("Tom"));

		// Then
		assertThat(result.isFailure()).isTrue();
		assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
	}

	@Test
	public void shouldDecorateCheckedFunctionAndReturnWithSuccess() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorldWithNameWithException("Tom")).willReturn("Hello world Tom");

		// When
		String result = AdaptiveBulkhead.decorateCheckedFunction(bulkhead, helloWorldService::returnHelloWorldWithNameWithException)
				.apply("Tom");

		// Then
		assertThat(result).isEqualTo("Hello world Tom");
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithNameWithException("Tom");
	}

	@Test
	public void shouldDecorateCheckedFunctionAndReturnWithException() throws Throwable {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorldWithNameWithException("Tom")).willThrow(new RuntimeException("BAM!"));

		// When
		CheckedFunction1<String, String> function = AdaptiveBulkhead.decorateCheckedFunction(bulkhead, helloWorldService::returnHelloWorldWithNameWithException);
		Try<String> result = Try.of(() -> function.apply("Tom"));

		// Then
		assertThat(result.isFailure()).isTrue();
		assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
	}

	@Test
	public void shouldReturnFailureWithBulkheadFullException() {
		// tag::bulkheadFullException[]
		// Given

		AdaptiveBulkheadConfig<MovingAverageConfig> config = AdaptiveBulkheadConfig.<MovingAverageConfig>builder().config(MovingAverageConfig.builder()
				.maxAcceptableRequestLatency(0.2)
				.desirableAverageThroughput(2)
				.desirableOperationLatency(0.1)
				.build()).build();

		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		bulkhead.tryAcquirePermission();
		bulkhead.tryAcquirePermission();

		// When
		CheckedRunnable checkedRunnable = AdaptiveBulkhead.decorateCheckedRunnable(bulkhead, () -> {
			throw new RuntimeException("BAM!");
		});
		Try result = Try.run(checkedRunnable);

		//Then
		assertThat(result.isFailure()).isTrue();
		assertThat(result.failed().get()).isInstanceOf(BulkheadFullException.class);
		// end::bulkheadFullException[]
	}

	@Test
	public void shouldReturnFailureWithRuntimeException() {

		// Given
		AdaptiveBulkheadConfig<MovingAverageConfig> config = AdaptiveBulkheadConfig.<MovingAverageConfig>builder().config(MovingAverageConfig.builder().maxAcceptableRequestLatency(0.2)
				.desirableAverageThroughput(2)
				.desirableOperationLatency(0.1)
				.build()).build();
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		bulkhead.tryAcquirePermission();

		//v When
		CheckedRunnable checkedRunnable = AdaptiveBulkhead.decorateCheckedRunnable(bulkhead, () -> {
			throw new RuntimeException("BAM!");
		});
		Try result = Try.run(checkedRunnable);

		//Then
		assertThat(result.isFailure()).isTrue();
		assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(0);
	}

	@Test
	public void shouldInvokeAsyncApply() throws ExecutionException, InterruptedException {
		// tag::shouldInvokeAsyncApply[]
		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

		// When
		Supplier<String> decoratedSupplier = AdaptiveBulkhead.decorateSupplier(bulkhead, () -> "This can be any method which returns: 'Hello");

		CompletableFuture<String> future = CompletableFuture.supplyAsync(decoratedSupplier)
				.thenApply(value -> value + " world'");

		String result = future.get();

		// Then
		assertThat(result).isEqualTo("This can be any method which returns: 'Hello world'");
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		// end::shouldInvokeAsyncApply[]
	}

	@Test
	public void shouldDecorateCompletionStageAndReturnWithSuccess() throws ExecutionException, InterruptedException {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello");

		// When

		Supplier<CompletionStage<String>> completionStageSupplier =
				() -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);

		Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
				AdaptiveBulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);

		CompletionStage<String> decoratedCompletionStage = decoratedCompletionStageSupplier
				.get()
				.thenApply(value -> value + " world");

		// Then
		assertThat(decoratedCompletionStage.toCompletableFuture().get()).isEqualTo("Hello world");
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
	}

	@Test
	public void shouldDecorateCompletionStageAndReturnWithExceptionAtSyncStage() throws ExecutionException, InterruptedException {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

		// When
		Supplier<CompletionStage<String>> completionStageSupplier = () -> {
			throw new WebServiceException("BAM! At sync stage");
		};

		Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
				AdaptiveBulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);

		// NOTE: Try.of does not detect a completion stage that has been completed with failure !
		Try<CompletionStage<String>> result = Try.of(decoratedCompletionStageSupplier::get);

		// Then the helloWorldService should be invoked 0 times
		BDDMockito.then(helloWorldService).should(times(0)).returnHelloWorld();
		assertThat(result.isSuccess()).isTrue();
		result.get()
				.exceptionally(
						error -> {
							// NOTE: Try.of does not detect a completion stage that has been completed with failure !
							assertThat(error).isInstanceOf(WebServiceException.class);
							return null;
						}
				);
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
	}

	@Test
	public void shouldDecorateCompletionStageAndReturnWithExceptionAtAsyncStage() throws ExecutionException, InterruptedException {

		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM! At async stage"));

		// When
		Supplier<CompletionStage<String>> completionStageSupplier =
				() -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
		Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
				AdaptiveBulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);
		CompletionStage<String> decoratedCompletionStage = decoratedCompletionStageSupplier.get();

		// Then the helloWorldService should be invoked 1 time
		assertThatThrownBy(decoratedCompletionStage.toCompletableFuture()::get)
				.isInstanceOf(ExecutionException.class).hasCause(new RuntimeException("BAM! At async stage"));
		BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
	}

	@Test
	public void shouldChainDecoratedFunctions() throws ExecutionException, InterruptedException {
		// tag::shouldChainDecoratedFunctions[]
		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
		AdaptiveBulkhead anotherBulkhead = AdaptiveBulkhead.of("testAnother", config);

		// When I create a Supplier and a Function which are decorated by different Bulkheads
		CheckedFunction0<String> decoratedSupplier
				= AdaptiveBulkhead.decorateCheckedSupplier(bulkhead, () -> "Hello");

		CheckedFunction1<String, String> decoratedFunction
				= AdaptiveBulkhead.decorateCheckedFunction(anotherBulkhead, (input) -> input + " world");

		// and I chain a function with map
		Try<String> result = Try.of(decoratedSupplier)
				.mapTry(decoratedFunction::apply);

		// Then
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.get()).isEqualTo("Hello world");
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		assertThat(anotherBulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		// end::shouldChainDecoratedFunctions[]
	}


	@Test
	public void shouldInvokeMap() {
		// tag::shouldInvokeMap[]
		// Given
		AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("testName", config);

		// When I decorate my function
		CheckedFunction0<String> decoratedSupplier = AdaptiveBulkhead.decorateCheckedSupplier(bulkhead, () -> "This can be any method which returns: 'Hello");

		// and chain an other function with map
		Try<String> result = Try.of(decoratedSupplier)
				.map(value -> value + " world'");

		// Then the Try Monad returns a Success<String>, if all functions ran successfully.
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.get()).isEqualTo("This can be any method which returns: 'Hello world'");
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
		// end::shouldInvokeMap[]
	}


}
