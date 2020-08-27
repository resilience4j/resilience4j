package io.github.resilience4j.bulkhead;

import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

public class VavrBulkheadTest {
    private HelloWorldService helloWorldService;
    private BulkheadConfig config;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
        config = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .build();
    }

    @Test
    public void shouldDecorateCheckedSupplierAndReturnWithSuccess() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        CheckedFunction0<String> checkedSupplier = VavrBulkhead
            .decorateCheckedSupplier(bulkhead, helloWorldService::returnHelloWorldWithException);

        String result = checkedSupplier.apply();

        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedSupplierAndReturnWithException() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException())
            .willThrow(new RuntimeException("BAM!"));
        CheckedFunction0<String> checkedSupplier = VavrBulkhead
            .decorateCheckedSupplier(bulkhead, helloWorldService::returnHelloWorldWithException);

        Try<String> result = Try.of(checkedSupplier);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithSuccess() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        VavrBulkhead.decorateCheckedRunnable(bulkhead, helloWorldService::sayHelloWorldWithException)
            .run();

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).sayHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithException() {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        CheckedRunnable checkedRunnable = VavrBulkhead.decorateCheckedRunnable(bulkhead, () -> {
            throw new RuntimeException("BAM!");
        });

        Try<Void> result = Try.run(checkedRunnable);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateCheckedConsumerAndReturnWithSuccess() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        VavrBulkhead.decorateCheckedConsumer(bulkhead,
            helloWorldService::sayHelloWorldWithNameWithException)
            .accept("Tom");

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).sayHelloWorldWithNameWithException("Tom");
    }

    @Test
    public void shouldDecorateCheckedConsumerAndReturnWithException() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        CheckedConsumer<String> checkedConsumer = VavrBulkhead
            .decorateCheckedConsumer(bulkhead, (value) -> {
                throw new RuntimeException("BAM!");
            });

        Try<Void> result = Try.run(() -> checkedConsumer.accept("Tom"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithSuccess() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithNameWithException("Tom"))
            .willReturn("Hello world Tom");

        String result = VavrBulkhead
            .decorateCheckedFunction(bulkhead,
                helloWorldService::returnHelloWorldWithNameWithException)
            .apply("Tom");

        assertThat(result).isEqualTo("Hello world Tom");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorldWithNameWithException("Tom");
    }

    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithException() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithNameWithException("Tom"))
            .willThrow(new RuntimeException("BAM!"));
        CheckedFunction1<String, String> function = VavrBulkhead
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
        BulkheadConfig config = BulkheadConfig.custom().maxConcurrentCalls(2).build();
        Bulkhead bulkhead = Bulkhead.of("test", config);
        bulkhead.tryAcquirePermission();
        bulkhead.tryAcquirePermission();
        CheckedRunnable checkedRunnable = VavrBulkhead.decorateCheckedRunnable(bulkhead, () -> {
            throw new RuntimeException("BAM!");
        });

        Try result = Try.run(checkedRunnable);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(BulkheadFullException.class);
        // end::bulkheadFullException[]
    }

    @Test
    public void shouldReturnFailureWithRuntimeException() {
        BulkheadConfig config = BulkheadConfig.custom().maxConcurrentCalls(2).build();
        Bulkhead bulkhead = Bulkhead.of("test", config);
        bulkhead.tryAcquirePermission();
        CheckedRunnable checkedRunnable = VavrBulkhead.decorateCheckedRunnable(bulkhead, () -> {
            throw new RuntimeException("BAM!");
        });

        Try result = Try.run(checkedRunnable);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldChainDecoratedFunctions() {
        // tag::shouldChainDecoratedFunctions[]
        Bulkhead bulkhead = Bulkhead.of("test", config);
        Bulkhead anotherBulkhead = Bulkhead.of("testAnother", config);
        // When I create a Supplier and a Function which are decorated by different Bulkheads
        CheckedFunction0<String> decoratedSupplier
            = VavrBulkhead.decorateCheckedSupplier(bulkhead, () -> "Hello");
        CheckedFunction1<String, String> decoratedFunction
            = VavrBulkhead.decorateCheckedFunction(anotherBulkhead, (input) -> input + " world");

        // and I chain a function with map
        Try<String> result = Try.of(decoratedSupplier)
            .mapTry(decoratedFunction);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        assertThat(anotherBulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        // end::shouldChainDecoratedFunctions[]
    }

    @Test
    public void shouldInvokeMap() {
        // tag::shouldInvokeMap[]
        Bulkhead bulkhead = Bulkhead.of("testName", config);
        // When I decorate my function
        CheckedFunction0<String> decoratedSupplier = VavrBulkhead.decorateCheckedSupplier(bulkhead,
            () -> "This can be any method which returns: 'Hello");

        // and chain an other function with map
        Try<String> result = Try.of(decoratedSupplier)
            .map(value -> value + " world'");

        // Then the Try Monad returns a Success<String>, if all functions ran successfully.
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("This can be any method which returns: 'Hello world'");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        // end::shouldInvokeMap[]
    }

    @Test
    public void shouldDecorateTrySupplierAndReturnWithSuccess() {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnTry()).willReturn(Try.success("Hello world"));

        Try<String> result = VavrBulkhead.executeTrySupplier(bulkhead, helloWorldService::returnTry);

        assertThat(result.get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnTry();
    }

    @Test
    public void shouldDecorateTrySupplierAndReturnWithException() {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnTry()).willReturn(Try.failure(new RuntimeException("BAM!")));

        Try<String> result = VavrBulkhead.executeTrySupplier(bulkhead, helloWorldService::returnTry);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnTry();
    }

    @Test
    public void shouldDecorateEitherSupplierAndReturnWithSuccess() {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnEither()).willReturn(Either.right("Hello world"));

        Either<Exception, String> result = VavrBulkhead
            .executeEitherSupplier(bulkhead, helloWorldService::returnEither);

        assertThat(result.get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnEither();
    }

    @Test
    public void shouldDecorateEitherSupplierAndReturnWithException() {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnEither()).willReturn(Either.left(new HelloWorldException()));

        Either<Exception, String> result = VavrBulkhead
            .executeEitherSupplier(bulkhead, helloWorldService::returnEither);

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnEither();
    }

    @Test
    public void shouldDecorateTrySupplierAndReturnWithBulkheadFullException() {
        Bulkhead bulkhead = mock(Bulkhead.class, RETURNS_DEEP_STUBS);
        given(bulkhead.tryAcquirePermission()).willReturn(false);

        Try<String> result = VavrBulkhead.decorateTrySupplier(bulkhead, helloWorldService::returnTry)
            .get();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(BulkheadFullException.class);
        then(helloWorldService).should(never()).returnTry();
    }

    @Test
    public void shouldDecorateEitherSupplierAndReturnWithBulkheadFullException() {
        Bulkhead bulkhead = mock(Bulkhead.class, RETURNS_DEEP_STUBS);
        given(bulkhead.tryAcquirePermission()).willReturn(false);

        Either<Exception, String> result = VavrBulkhead
            .decorateEitherSupplier(bulkhead, helloWorldService::returnEither).get();

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isInstanceOf(BulkheadFullException.class);
        then(helloWorldService).should(never()).returnEither();
    }
}
