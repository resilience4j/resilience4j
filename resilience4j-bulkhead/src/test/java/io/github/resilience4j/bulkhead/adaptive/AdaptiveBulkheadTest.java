package io.github.resilience4j.bulkhead.adaptive;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.core.functions.CheckedConsumer;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

public class AdaptiveBulkheadTest {

    private AdaptiveBulkhead bulkhead;
    private AdaptiveBulkheadConfig config;
    private HelloWorldService helloWorldService;

    @Before
    public void setUp() {
        helloWorldService = Mockito.mock(HelloWorldService.class);
        config = AdaptiveBulkheadConfig.custom()
            .maxConcurrentCalls(2)
            .minConcurrentCalls(1)
            .initialConcurrentCalls(1)
            .slowCallDurationThreshold(Duration.ofMillis(100))
            .build();
        bulkhead = AdaptiveBulkhead.of("test", config);
    }

    @Test
    public void shouldReturnTheCorrectName() {
        assertThat(bulkhead.getName()).isEqualTo("test");
    }

    @Test
    public void testToString() {
        assertThat(bulkhead.toString()).isEqualTo("AdaptiveBulkhead 'test'");
    }

    @Test
    public void testCreateWithNullConfig() {
        assertThatThrownBy(() -> AdaptiveBulkhead.of("test", () -> null))
            .isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
    }

    @Test
    public void testCreateWithDefaults() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.ofDefaults("test");

        assertThat(bulkhead).isNotNull();
        assertThat(bulkhead.getBulkheadConfig()).isNotNull();
        assertThat(bulkhead.getBulkheadConfig().getSlidingWindowSize()).isEqualTo(100);
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithSuccess() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        Supplier<String> supplier = AdaptiveBulkhead
            .decorateSupplier(bulkhead, helloWorldService::returnHelloWorld);

        assertThat(supplier.get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorld();
    }

    @Test
    public void shouldExecuteSupplierAndReturnWithSuccess() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        String result = bulkhead.executeSupplier(helloWorldService::returnHelloWorld);

        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorld();
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithException() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new RuntimeException("BAM!"));
        Supplier<String> supplier = AdaptiveBulkhead
            .decorateSupplier(bulkhead, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(supplier::get);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorld();
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithExceptionAdaptIfError() {
        final AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .minConcurrentCalls(2)
            .slowCallDurationThreshold(Duration.ofMillis(200))
            .recordExceptions(RuntimeException.class)
            .build();
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new RuntimeException("BAM!"));
        Supplier<String> supplier = AdaptiveBulkhead
            .decorateSupplier(bulkhead, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(supplier::get);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
        then(helloWorldService).should().returnHelloWorld();
    }

    @Test
    public void shouldDecorateCheckedSupplierAndReturnWithSuccess() throws Throwable {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException())
            .willReturn("Hello world");
        CheckedSupplier<String> checkedSupplier = AdaptiveBulkhead
            .decorateCheckedSupplier(bulkhead, helloWorldService::returnHelloWorldWithException);

        String result = checkedSupplier.get();

        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedSupplierAndReturnWithException() throws Throwable {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException())
            .willThrow(new RuntimeException("BAM!"));
        CheckedSupplier<String> checkedSupplier = AdaptiveBulkhead
            .decorateCheckedSupplier(bulkhead, helloWorldService::returnHelloWorldWithException);

        Try<String> result = Try.of(checkedSupplier::get);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedSupplierAndReturnWithExceptionAdaptIfError() throws Throwable {
        final AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .minConcurrentCalls(2)
            .slowCallDurationThreshold(Duration.ofMillis(200))
            .recordExceptions(RuntimeException.class)
            .build();
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException())
            .willThrow(new RuntimeException("BAM!"));
        CheckedSupplier<String> checkedSupplier = AdaptiveBulkhead
            .decorateCheckedSupplier(bulkhead, helloWorldService::returnHelloWorldWithException);

        Try<String> result = Try.of(checkedSupplier::get);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCallableAndReturnWithSuccess() throws Throwable {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException())
            .willReturn("Hello world");
        Callable<String> callable = AdaptiveBulkhead
            .decorateCallable(bulkhead, helloWorldService::returnHelloWorldWithException);

        String result = callable.call();

        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    public void shouldExecuteCallableAndReturnWithSuccess() throws Throwable {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException())
            .willReturn("Hello world");

        String result = bulkhead.executeCallable(helloWorldService::returnHelloWorldWithException);

        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCallableAndReturnWithException() throws Throwable {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException())
            .willThrow(new RuntimeException("BAM!"));
        Callable<String> callable = AdaptiveBulkhead
            .decorateCallable(bulkhead, helloWorldService::returnHelloWorldWithException);

        Try<String> result = Try.of(callable::call);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCallableAndReturnWithExceptionIfError() throws Throwable {
        final AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .minConcurrentCalls(2)
            .slowCallDurationThreshold(Duration.ofMillis(200))
            .recordExceptions(RuntimeException.class)
            .build();
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException())
            .willThrow(new RuntimeException("BAM!"));
        Callable<String> callable = AdaptiveBulkhead
            .decorateCallable(bulkhead, helloWorldService::returnHelloWorldWithException);

        Try<String> result = Try.of(callable::call);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithSuccess() throws Throwable {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

        AdaptiveBulkhead
            .decorateCheckedRunnable(bulkhead, helloWorldService::sayHelloWorldWithException)
            .run();

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().sayHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithException() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        CheckedRunnable checkedRunnable = AdaptiveBulkhead.decorateCheckedRunnable(bulkhead, () -> {
            throw new RuntimeException("BAM!");
        });

        Try<Void> result = Try.run(checkedRunnable::run);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithExceptionAdaptIfError() {
        final AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .minConcurrentCalls(2)
            .slowCallDurationThreshold(Duration.ofMillis(200))
            .recordExceptions(RuntimeException.class)
            .build();
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        CheckedRunnable checkedRunnable = AdaptiveBulkhead.decorateCheckedRunnable(bulkhead, () -> {
            throw new RuntimeException("BAM!");
        });

        Try<Void> result = Try.run(checkedRunnable::run);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
    }

    @Test
    public void shouldDecorateRunnableAndReturnWithSuccess() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

        AdaptiveBulkhead.decorateRunnable(bulkhead, helloWorldService::sayHelloWorld).run();

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().sayHelloWorld();
    }

    @Test
    public void shouldExecuteRunnableAndReturnWithSuccess() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

        bulkhead.executeRunnable(helloWorldService::sayHelloWorld);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().sayHelloWorld();
    }

    @Test
    public void shouldDecorateRunnableAndReturnWithException() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        Runnable runnable = AdaptiveBulkhead.decorateRunnable(bulkhead, () -> {
            throw new RuntimeException("BAM!");
        });

        Try<Void> result = Try.run(runnable::run);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateRunnableAndReturnWithExceptionAdaptIfError() {
        final AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .minConcurrentCalls(2)
            .slowCallDurationThreshold(Duration.ofMillis(200))
            .recordExceptions(RuntimeException.class)
            .build();
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        Runnable runnable = AdaptiveBulkhead.decorateRunnable(bulkhead, () -> {
            throw new RuntimeException("BAM!");
        });

        Try<Void> result = Try.run(runnable::run);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
    }

    @Test
    public void shouldDecorateConsumerAndReturnWithSuccess() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

        AdaptiveBulkhead.decorateConsumer(bulkhead, helloWorldService::sayHelloWorldWithName)
            .accept("Tom");

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().sayHelloWorldWithName("Tom");
    }

    @Test
    public void shouldDecorateConsumerAndReturnWithException() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        Consumer<String> consumer = AdaptiveBulkhead.decorateConsumer(bulkhead, (value) -> {
            throw new RuntimeException("BAM!");
        });

        Try<Void> result = Try.run(() -> consumer.accept("Tom"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateConsumerAndReturnWithExceptionAdaptIfError() {
        final AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .minConcurrentCalls(2)
            .slowCallDurationThreshold(Duration.ofMillis(100))
            .recordExceptions(RuntimeException.class)
            .build();
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        Consumer<String> consumer = AdaptiveBulkhead.decorateConsumer(bulkhead, (value) -> {
            throw new RuntimeException("BAM!");
        });

        Try<Void> result = Try.run(() -> consumer.accept("Tom"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
    }

    @Test
    public void shouldDecorateCheckedConsumerAndReturnWithSuccess() throws Throwable {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);

        AdaptiveBulkhead.decorateCheckedConsumer(bulkhead,
            helloWorldService::sayHelloWorldWithNameWithException)
            .accept("Tom");

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().sayHelloWorldWithNameWithException("Tom");
    }

    @Test
    public void shouldDecorateCheckedConsumerAndReturnWithException() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        CheckedConsumer<String> checkedConsumer = AdaptiveBulkhead
            .decorateCheckedConsumer(bulkhead, (value) -> {
                throw new RuntimeException("BAM!");
            });

        Try<Void> result = Try.run(() -> checkedConsumer.accept("Tom"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateFunctionAndReturnWithSuccess() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithName("Tom"))
            .willReturn("Hello world Tom");
        Function<String, String> function = AdaptiveBulkhead
            .decorateFunction(bulkhead, helloWorldService::returnHelloWorldWithName);

        String result = function.apply("Tom");

        assertThat(result).isEqualTo("Hello world Tom");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorldWithName("Tom");
    }

    @Test
    public void shouldDecorateFunctionAndReturnWithException() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithName("Tom"))
            .willThrow(new RuntimeException("BAM!"));
        Function<String, String> function = AdaptiveBulkhead
            .decorateFunction(bulkhead, helloWorldService::returnHelloWorldWithName);

        Try<String> result = Try.of(() -> function.apply("Tom"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithSuccess() throws Throwable {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithNameWithException("Tom"))
            .willReturn("Hello world Tom");

        String result = AdaptiveBulkhead.decorateCheckedFunction(bulkhead,
            helloWorldService::returnHelloWorldWithNameWithException)
            .apply("Tom");

        assertThat(result).isEqualTo("Hello world Tom");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should()
            .returnHelloWorldWithNameWithException("Tom");
    }

    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithException() throws IOException {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithNameWithException("Tom"))
            .willThrow(new RuntimeException("BAM!"));
        CheckedFunction<String, String> function = AdaptiveBulkhead
            .decorateCheckedFunction(bulkhead,
                helloWorldService::returnHelloWorldWithNameWithException);

        Try<String> result = Try.of(() -> function.apply("Tom"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldReturnFailureWithBulkheadFullException() {
        // tag::bulkheadFullException[]
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .minConcurrentCalls(2)
            .slowCallDurationThreshold(Duration.ofMillis(2))
            .build();
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        bulkhead.acquirePermission();
        bulkhead.acquirePermission();
        CheckedRunnable checkedRunnable = AdaptiveBulkhead.decorateCheckedRunnable(bulkhead, () -> {
            throw new RuntimeException("BAM!");
        });

        Try<?> result = Try.run(checkedRunnable::run);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(BulkheadFullException.class);
        // end::bulkheadFullException[]
    }

    @Test
    public void shouldReturnFailureWithRuntimeException() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .minConcurrentCalls(2)
            .slowCallDurationThreshold(Duration.ofMillis(2))
            .build();
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        bulkhead.tryAcquirePermission();
        CheckedRunnable checkedRunnable = AdaptiveBulkhead.decorateCheckedRunnable(bulkhead, () -> {
            throw new RuntimeException("BAM!");
        });

        Try<?> result = Try.run(checkedRunnable::run);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldInvokeAsyncApply() throws ExecutionException, InterruptedException {
        // tag::shouldInvokeAsyncApply[]
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        Supplier<String> decoratedSupplier = AdaptiveBulkhead
            .decorateSupplier(bulkhead, () -> "This can be any method which returns: 'Hello");
        CompletableFuture<String> future = CompletableFuture.supplyAsync(decoratedSupplier)
            .thenApply(value -> value + " world'");

        String result = future.get();

        assertThat(result).isEqualTo("This can be any method which returns: 'Hello world'");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        // end::shouldInvokeAsyncApply[]
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithSuccess()
        throws ExecutionException, InterruptedException {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello");
        Supplier<CompletionStage<String>> completionStageSupplier =
            () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
            AdaptiveBulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);

        CompletionStage<String> decoratedCompletionStage = decoratedCompletionStageSupplier.get()
            .thenApply(value -> value + " world");

        assertThat(decoratedCompletionStage.toCompletableFuture().get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorld();
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithExceptionAtSyncStage() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        Supplier<CompletionStage<String>> completionStageSupplier = () -> {
            throw new WebServiceException("BAM! At sync stage");
        };
        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
            AdaptiveBulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);

        // NOTE: Try.of does not detect a completion stage that has been completed with failure !
        Try<CompletionStage<String>> result = Try.of(decoratedCompletionStageSupplier::get);

        // Then the helloWorldService should be invoked 0 times
        then(helloWorldService).should(never()).returnHelloWorld();
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
    public void shouldDecorateCompletionStageAndReturnWithExceptionAtSyncStageAdaptIfError() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .minConcurrentCalls(2)
            .slowCallDurationThreshold(Duration.ofMillis(2))
            .build();
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        Supplier<CompletionStage<String>> completionStageSupplier = () -> {
            throw new WebServiceException("BAM! At sync stage");
        };
        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
            AdaptiveBulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);

        // NOTE: Try.of does not detect a completion stage that has been completed with failure !
        Try<CompletionStage<String>> result = Try.of(decoratedCompletionStageSupplier::get);

        then(helloWorldService).should(never()).returnHelloWorld();
        assertThat(result.isSuccess()).isTrue();
        result.get()
            .exceptionally(
                error -> {
                    // NOTE: Try.of does not detect a completion stage that has been completed with failure !
                    assertThat(error).isInstanceOf(WebServiceException.class);
                    return null;
                }
            );
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithExceptionAtAsyncStage() {
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new RuntimeException("BAM! At async stage"));
        Supplier<CompletionStage<String>> completionStageSupplier =
            () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
            AdaptiveBulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);

        CompletionStage<String> decoratedCompletionStage = decoratedCompletionStageSupplier.get();

        assertThatThrownBy(decoratedCompletionStage.toCompletableFuture()::get)
            .isInstanceOf(ExecutionException.class)
            .hasCause(new RuntimeException("BAM! At async stage"));
        then(helloWorldService).should().returnHelloWorld();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldChainDecoratedFunctions() {
        // tag::shouldChainDecoratedFunctions[]
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("test", config);
        AdaptiveBulkhead anotherBulkhead = AdaptiveBulkhead.of("testAnother", config);
        // Given a Supplier and a Function which are decorated by different bulkheads
        CheckedSupplier<String> decoratedSupplier = AdaptiveBulkhead
            .decorateCheckedSupplier(bulkhead, () -> "Hello");
        CheckedFunction<String, String> decoratedFunction = AdaptiveBulkhead
            .decorateCheckedFunction(anotherBulkhead, (input) -> input + " world");

        Try<String> result = Try.of(decoratedSupplier::get).mapTry(decoratedFunction::apply);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        assertThat(anotherBulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        // end::shouldChainDecoratedFunctions[]
    }


    @Test
    public void shouldInvokeMap() {
        // tag::shouldInvokeMap[]
        AdaptiveBulkhead bulkhead = AdaptiveBulkhead.of("testName", config);
        CheckedSupplier<String> decoratedSupplier = AdaptiveBulkhead.decorateCheckedSupplier(
            bulkhead, () -> "This can be any method which returns: 'Hello");

        Try<String> result = Try.of(decoratedSupplier::get)
            .map(value -> value + " world'");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("This can be any method which returns: 'Hello world'");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        // end::shouldInvokeMap[]
    }

}
